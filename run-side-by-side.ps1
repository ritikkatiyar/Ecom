param(
  [switch]$StartInfra = $true,
  [switch]$StartFrontend = $true,
  [switch]$StartBackend = $true,
  [switch]$SkipSharedInstall = $false,
  [switch]$UseJobs = $true,
  [string]$EnvFile = ""
)

$ErrorActionPreference = "Stop"

function Start-TerminalCommand([string]$title, [string]$workingDir, [string]$command) {
  $full = "Set-Location '$workingDir'; $command"
  Start-Process powershell -ArgumentList "-NoExit", "-Command", "$host.ui.RawUI.WindowTitle='$title'; $full"
}

function Start-JobCommand([string]$name, [string]$workingDir, [string]$command, [string]$logPath, [string]$errPath) {
  $existing = Get-Job -Name $name -ErrorAction SilentlyContinue
  if ($existing) {
    $existing | Stop-Job -ErrorAction SilentlyContinue
    $existing | Remove-Job -Force -ErrorAction SilentlyContinue
  }

  Start-Job -Name $name -ArgumentList $workingDir, $command, $logPath, $errPath -ScriptBlock {
    param($wd, $cmd, $log, $err)
    Set-Location $wd
    Invoke-Expression "$cmd 1>> '$log' 2>> '$err'"
  } | Out-Null
}

function Test-CommandAvailable([string]$name) {
  return [bool](Get-Command $name -ErrorAction SilentlyContinue)
}

function Resolve-MavenCommand {
  $mvn = Get-Command mvn -ErrorAction SilentlyContinue
  if ($mvn) {
    return "mvn"
  }

  if ($env:MAVEN_HOME) {
    $candidate = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
    if (Test-Path $candidate) {
      return "`"$candidate`""
    }
  }

  $commonCandidate = "C:\Tools\apache-maven-3.9.9\bin\mvn.cmd"
  if (Test-Path $commonCandidate) {
    return "`"$commonCandidate`""
  }

  return $null
}

function Import-EnvFile([string]$path) {
  if (-not (Test-Path $path)) {
    return 0
  }

  $count = 0
  foreach ($line in Get-Content $path) {
    $trimmed = $line.Trim()
    if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
      continue
    }

    if ($trimmed -match "^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$") {
      $key = $matches[1]
      $value = $matches[2].Trim()

      if (($value.StartsWith("'") -and $value.EndsWith("'")) -or ($value.StartsWith('"') -and $value.EndsWith('"'))) {
        $value = $value.Substring(1, $value.Length - 2)
      }

      if (-not (Test-Path "Env:$key")) {
        Set-Item -Path "Env:$key" -Value $value
        $count++
      }
    }
  }

  return $count
}

function Test-TcpPortOpen([string]$targetHost, [int]$port, [int]$timeoutMs = 2000) {
  $client = New-Object System.Net.Sockets.TcpClient
  try {
    $iar = $client.BeginConnect($targetHost, $port, $null, $null)
    $connected = $iar.AsyncWaitHandle.WaitOne($timeoutMs, $false)
    if (-not $connected) {
      return $false
    }
    $client.EndConnect($iar) | Out-Null
    return $true
  } catch {
    return $false
  } finally {
    $client.Close()
  }
}

function Wait-ForInfraDependency([string]$name, [string]$targetHost, [int]$port, [int]$maxWaitSeconds = 120) {
  $deadline = (Get-Date).AddSeconds($maxWaitSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-TcpPortOpen -targetHost $targetHost -port $port) {
      Write-Host "$name is reachable at ${targetHost}:$port"
      return
    }
    Start-Sleep -Seconds 2
  }
  throw "Timed out waiting for $name at ${targetHost}:$port"
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $repoRoot "ecom-back"
$frontendDir = Join-Path $repoRoot "ecom-storefront"
$buildArtifactsDir = Join-Path $repoRoot "build-artifacts"
if (-not (Test-Path $buildArtifactsDir)) {
  New-Item -Path $buildArtifactsDir -ItemType Directory | Out-Null
}

$envFilesToLoad = @()
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
  $envFilesToLoad = @(
    (Join-Path $repoRoot ".env.local"),
    (Join-Path $repoRoot ".env"),
    (Join-Path $backendDir ".env"),
    (Join-Path $backendDir "services\product-service\.env")
  )
} else {
  $envFilesToLoad = @($EnvFile)
}

foreach ($file in $envFilesToLoad) {
  $loaded = Import-EnvFile $file
  if ($loaded -gt 0) {
    Write-Host "Loaded $loaded environment variable(s) from $file"
  }
}

if ($StartInfra) {
  if (-not (Test-CommandAvailable "docker")) {
    throw "Docker is not installed or not available in PATH."
  }
  Write-Host "Starting infrastructure..."
  docker compose -f "$backendDir/infrastructure/docker-compose.yml" up -d
}

if ($StartBackend) {
  Write-Host "Waiting for core infra dependencies..."
  Wait-ForInfraDependency "MySQL" "127.0.0.1" 3306 150
  Wait-ForInfraDependency "Redis" "127.0.0.1" 6379 120
  Wait-ForInfraDependency "Kafka" "127.0.0.1" 9092 150
  Wait-ForInfraDependency "MongoDB" "127.0.0.1" 27017 120

  $mvnCommand = Resolve-MavenCommand
  if (-not $mvnCommand) {
    Write-Warning "Maven (mvn) is not installed. Install Maven, then rerun this script."
  } else {
    if (-not $SkipSharedInstall) {
      Write-Host "Installing shared modules..."
      Push-Location $backendDir
      try {
        Invoke-Expression "$mvnCommand -pl common/common-core,common/common-events,common/common-security,common/common-redis,common/common-web -am -DskipTests install"
        if ($LASTEXITCODE -ne 0) {
          throw "Shared module installation failed."
        }
      } finally {
        Pop-Location
      }
    }

    $serviceDefinitions = @(
      @{ title = "auth-service"; command = "$mvnCommand -f services/auth-service/pom.xml spring-boot:run" },
      @{ title = "user-service"; command = "$mvnCommand -f services/user-service/pom.xml spring-boot:run" },
      @{ title = "product-service"; command = "$mvnCommand -f services/product-service/pom.xml spring-boot:run" },
      @{ title = "inventory-service"; command = "$mvnCommand -f services/inventory-service/pom.xml spring-boot:run" },
      @{ title = "cart-service"; command = "$mvnCommand -f services/cart-service/pom.xml spring-boot:run" },
      @{ title = "order-service"; command = "$mvnCommand -f services/order-service/pom.xml spring-boot:run" },
      @{ title = "payment-service"; command = "$mvnCommand -f services/payment-service/pom.xml spring-boot:run" },
      @{ title = "review-service"; command = "$mvnCommand -f services/review-service/pom.xml spring-boot:run" },
      @{ title = "search-service"; command = "$mvnCommand -f services/search-service/pom.xml spring-boot:run" },
      @{ title = "notification-service"; command = "$mvnCommand -f services/notification-service/pom.xml spring-boot:run" },
      @{ title = "api-gateway"; command = "$mvnCommand -f api-gateway/pom.xml spring-boot:run" }
    )

    foreach ($service in $serviceDefinitions) {
      if ($UseJobs) {
        $serviceName = $service.title -replace "-service$", ""
        $serviceLog = Join-Path $buildArtifactsDir "$serviceName-startup.log"
        $serviceErr = Join-Path $buildArtifactsDir "$serviceName-startup.err.log"
        Start-JobCommand $service.title $backendDir $service.command $serviceLog $serviceErr
      } else {
        Start-TerminalCommand $service.title $backendDir $service.command
      }
      # Avoid a startup thundering herd against infra dependencies.
      Start-Sleep -Seconds 2
    }
  }
}

if ($StartFrontend) {
  if (-not (Test-CommandAvailable "npm")) {
    throw "npm is not installed or not available in PATH."
  }
  if (-not (Test-Path "$frontendDir/node_modules")) {
    Write-Host "Installing frontend dependencies..."
    npm --prefix "$frontendDir" install
  }

  if ($UseJobs) {
    $frontLog = Join-Path $buildArtifactsDir "storefront-startup.log"
    $frontErr = Join-Path $buildArtifactsDir "storefront-startup.err.log"
    Start-JobCommand "ecom-storefront" $frontendDir "npm run dev" $frontLog $frontErr
  } else {
    Start-TerminalCommand "ecom-storefront" $frontendDir "npm run dev"
  }
}

Write-Host "Side-by-side dev startup triggered."
if ($UseJobs) {
  Write-Host ""
  Write-Host "Running in integrated terminal mode (PowerShell jobs)."
  Write-Host "Check jobs: Get-Job"
  Write-Host "Stream logs: Receive-Job -Name <job-name> -Keep"
  Write-Host "Stop all: Get-Job | Stop-Job; Get-Job | Remove-Job"
}
