param(
  [ValidateSet("start", "stop", "restart", "status")]
  [string]$Action = "start",
  [switch]$StartInfra = $true,
  [switch]$StartFrontend = $true,
  [switch]$StartBackend = $true,
  [switch]$EnableRedis = $false,
  [switch]$EnableCheckout = $false,
  [switch]$EnableReviews = $false,
  [switch]$EnableNotifications = $false,
  [switch]$EnableSearch = $false,
  [switch]$EnableKafkaUi = $false,
  [switch]$EnableObservability = $false,
  [switch]$SkipSharedInstall = $false,
  [switch]$SkipPreflight = $false,
  [switch]$SkipCloudinaryCheck = $false,
  [switch]$UseJobs = $false,
  [string]$EnvFile = "",
  [switch]$StopInfra = $true,
  [switch]$CleanFrontendCache = $true,
  [switch]$UseDetachedProcesses = $true,
  [int]$LogRetentionDays = 7
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

function Load-ProcessMap([string]$path) {
  if (-not (Test-Path $path)) {
    return @{}
  }

  try {
    $raw = Get-Content -Path $path -Raw
    if ([string]::IsNullOrWhiteSpace($raw)) {
      return @{}
    }
    $parsed = ConvertFrom-Json $raw -ErrorAction Stop
    $map = @{}
    foreach ($item in $parsed.PSObject.Properties) {
      $map[$item.Name] = [int]$item.Value
    }
    return $map
  } catch {
    return @{}
  }
}

function Save-ProcessMap([string]$path, [hashtable]$map) {
  $dir = Split-Path -Parent $path
  if (-not (Test-Path $dir)) {
    New-Item -Path $dir -ItemType Directory | Out-Null
  }

  $json = $map | ConvertTo-Json -Depth 3
  Set-Content -Path $path -Value $json
}

function Cleanup-OldLogFiles([string]$directory, [int]$retentionDays = 7) {
  if (-not (Test-Path $directory) -or $retentionDays -lt 0) {
    return
  }

  $cutoff = (Get-Date).AddDays(-$retentionDays)
  Get-ChildItem -Path $directory -File -Filter *.log -ErrorAction SilentlyContinue |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    ForEach-Object {
      try {
        Remove-Item $_.FullName -Force -ErrorAction Stop
      } catch {
        Write-Warning "Unable to remove old log file $($_.FullName): $($_.Exception.Message)"
      }
    }
}

function Test-ProcessAlive([int]$processId) {
  if ($processId -le 0) {
    return $false
  }
  try {
    $process = Get-Process -Id $processId -ErrorAction Stop
    return $null -ne $process
  } catch {
    return $false
  }
}

function Stop-TrackedProcess([string]$name, [hashtable]$map) {
  if (-not $map.ContainsKey($name)) {
    return
  }

  $processId = [int]$map[$name]
  if (Test-ProcessAlive $processId) {
    try {
      Stop-Process -Id $processId -Force -ErrorAction Stop
    } catch {
      Write-Warning "Unable to stop process $name (PID $processId): $($_.Exception.Message)"
    }
  }
  $map.Remove($name) | Out-Null
}

function Start-DetachedCommand([string]$name, [string]$workingDir, [string]$command, [string]$logPath, [string]$errPath, [string]$processMapPath) {
  $map = Load-ProcessMap $processMapPath
  Stop-TrackedProcess $name $map

  if (Test-Path $logPath) {
    Remove-Item $logPath -Force -ErrorAction SilentlyContinue
  }
  if (Test-Path $errPath) {
    Remove-Item $errPath -Force -ErrorAction SilentlyContinue
  }

  $wrappedCommand = "`$ErrorActionPreference='Stop'; Set-Location '$workingDir'; $command"
  $process = Start-Process `
    -FilePath "powershell.exe" `
    -WorkingDirectory $workingDir `
    -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $wrappedCommand `
    -RedirectStandardOutput $logPath `
    -RedirectStandardError $errPath `
    -WindowStyle Hidden `
    -PassThru

  $map[$name] = $process.Id
  Save-ProcessMap $processMapPath $map
}

function Test-CommandAvailable([string]$name) {
  return [bool](Get-Command $name -ErrorAction SilentlyContinue)
}

function Assert-CommandAvailable([string]$name, [string]$hint) {
  if (-not (Test-CommandAvailable $name)) {
    throw "$name is not available in PATH. $hint"
  }
}

function Assert-EnvHasValue([string]$name, [string]$hint) {
  $value = [Environment]::GetEnvironmentVariable($name)
  if ([string]::IsNullOrWhiteSpace($value)) {
    throw "Missing required environment variable: $name. $hint"
  }
}

function Assert-DockerDaemon {
  try {
    $null = docker version --format '{{.Server.Version}}' 2>$null
    if ($LASTEXITCODE -ne 0) {
      throw "docker version returned exit code $LASTEXITCODE"
    }
  } catch {
    throw "Docker daemon is not reachable. Start Docker Desktop and retry."
  }
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

function Import-EnvFile([string]$path, [switch]$OverwriteExisting = $true) {
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

      if ($OverwriteExisting -or -not (Test-Path "Env:$key")) {
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

function Wait-ForMySqlReady([string]$containerName = "ecom-mysql", [int]$maxWaitSeconds = 180) {
  $deadline = (Get-Date).AddSeconds($maxWaitSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      $isRunningRaw = docker inspect -f "{{.State.Running}}" $containerName 2>$null
      $isRunning = ""
      if (-not [string]::IsNullOrWhiteSpace($isRunningRaw)) {
        $isRunning = $isRunningRaw.ToString().Trim().ToLowerInvariant()
      }
      if ($LASTEXITCODE -eq 0 -and $isRunning -eq "true") {
        docker exec $containerName mysqladmin ping -h 127.0.0.1 -uroot -proot --silent 1>$null 2>$null
        if ($LASTEXITCODE -eq 0) {
          Write-Host "MySQL is accepting authenticated connections in container '$containerName'"
          return
        }

        # Fallback: container log readiness when mysqladmin probing is flaky.
        $mysqlLogs = docker logs --tail 80 $containerName 2>$null
        if ($LASTEXITCODE -eq 0 -and ($mysqlLogs -match "ready for connections")) {
          Write-Host "MySQL reported readiness in container logs for '$containerName'"
          return
        }
      }
    } catch {
      # Keep polling until MySQL is fully initialized.
    }
    Start-Sleep -Seconds 2
  }
  Write-Warning "Timed out waiting for strict MySQL readiness in container '$containerName'. Continuing startup and relying on service health retries."
}

function Wait-ForHttpDependency([string]$name, [string]$url, [int]$maxWaitSeconds = 180) {
  $deadline = (Get-Date).AddSeconds($maxWaitSeconds)
  while ((Get-Date) -lt $deadline) {
    try {
      # curl.exe is more stable than Invoke-WebRequest in some Windows/proxy environments.
      $null = curl.exe -s -m 5 $url
      if ($LASTEXITCODE -eq 0) {
        Write-Host "$name is healthy at $url"
        return
      }
    } catch {
      # Keep polling until the dependency becomes fully ready.
    }
    Start-Sleep -Seconds 2
  }
  throw "Timed out waiting for $name health endpoint at $url"
}

function Test-HttpOk([string]$url) {
  try {
    $statusCode = curl.exe -s -o NUL -w "%{http_code}" -m 15 $url
    return $statusCode -match "^2\d\d$"
  } catch {
    return $false
  }
}

function Test-AnyProjectPortListening {
  $ports = @(3000, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090)
  foreach ($port in $ports) {
    $matches = netstat -ano -p tcp | Select-String -Pattern "^\s*TCP\s+\S+:$port\s+\S+\s+LISTENING\s+(\d+)\s*$"
    if ($matches.Count -gt 0) {
      return $true
    }
  }
  return $false
}

function Wait-ForServiceHealth([string]$name, [string]$url, [int]$maxWaitSeconds = 180) {
  $deadline = (Get-Date).AddSeconds($maxWaitSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-HttpOk $url) {
      Write-Host "$name health check passed at $url"
      return $true
    }
    Start-Sleep -Seconds 2
  }
  return $false
}

function Ensure-ServiceHealthyWithRestart(
  [string]$name,
  [string]$url,
  [string]$workingDir,
  [string]$command,
  [string]$logPath,
  [string]$errPath,
  [switch]$UseDetached,
  [string]$ProcessMapPath = "",
  [int]$maxAttempts = 3,
  [int]$waitSecondsPerAttempt = 120
) {
  for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
    if (Wait-ForServiceHealth $name $url $waitSecondsPerAttempt) {
      return $true
    }

    if ($attempt -lt $maxAttempts) {
      Write-Warning "$name failed health check (attempt $attempt/$maxAttempts). Restarting..."
      Start-Sleep -Seconds 5
      if ($UseDetached) {
        Start-DetachedCommand $name $workingDir $command $logPath $errPath $ProcessMapPath
      } else {
        Start-JobCommand $name $workingDir $command $logPath $errPath
      }
    }
  }

  return $false
}

function Ensure-SolrHealthy([string]$composeFilePath) {
  $primaryUrl = "http://127.0.0.1:8983/solr/"
  $healthUrl = "http://127.0.0.1:8983/solr/admin/info/system"

  Wait-ForInfraDependency "Solr" "127.0.0.1" 8983 300
  try {
    Wait-ForHttpDependency "Solr" $primaryUrl 300
    Wait-ForHttpDependency "Solr admin" $healthUrl 300
    return
  } catch {
    Write-Warning "Solr accepted TCP but did not become HTTP healthy. Restarting solr container once..."
    docker compose -f $composeFilePath restart solr | Out-Null
    Wait-ForInfraDependency "Solr" "127.0.0.1" 8983 300
    Wait-ForHttpDependency "Solr" $primaryUrl 300
    Wait-ForHttpDependency "Solr admin" $healthUrl 300
  }
}

function Invoke-PreflightChecks {
  if ($StartInfra) {
    Assert-CommandAvailable "docker" "Install Docker Desktop."
    Assert-DockerDaemon
  }

  if ($StartBackend) {
    Assert-CommandAvailable "mvn" "Install Maven and ensure mvn is on PATH."
    if (-not $SkipCloudinaryCheck) {
      Assert-EnvHasValue "CLOUDINARY_CLOUD_NAME" "Set it in ecom-back/services/product-service/.env.local or your shell env."
      Assert-EnvHasValue "CLOUDINARY_API_KEY" "Set it in ecom-back/services/product-service/.env.local or your shell env."
      Assert-EnvHasValue "CLOUDINARY_API_SECRET" "Set it in ecom-back/services/product-service/.env.local or your shell env."
    }
  }

  if ($StartFrontend) {
    Assert-CommandAvailable "npm" "Install Node.js and npm."
  }
}

function Get-InfraServices {
  $services = @("mysql", "kafka")
  if ([string]::IsNullOrWhiteSpace($env:PRODUCT_MONGODB_URI)) {
    $services += "mongodb"
  }
  if ($EnableRedis) {
    $services += "redis"
  }
  if ($EnableKafkaUi) {
    $services += "kafka-ui"
  }
  if ($EnableSearch) {
    $services += "solr"
  }
  if ($EnableObservability) {
    $services += @("zipkin", "alertmanager", "prometheus", "grafana")
  }
  return $services
}

function Get-BackendServiceDefinitions([string]$mvnCommand) {
  $allServices = @(
    @{ title = "auth-service"; command = "$mvnCommand -f services/auth-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8081/actuator/health"; healthWaitSeconds = 120; healthAttempts = 3; enabled = $true },
    @{ title = "user-service"; command = "$mvnCommand -f services/user-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8082/actuator/health"; healthWaitSeconds = 120; healthAttempts = 3; enabled = $true },
    @{ title = "product-service"; command = "$mvnCommand -f services/product-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8083/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $true },
    @{ title = "inventory-service"; command = "$mvnCommand -f services/inventory-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8084/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $true },
    @{ title = "cart-service"; command = "$mvnCommand -f services/cart-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8085/actuator/health"; healthWaitSeconds = 240; healthAttempts = 3; enabled = $true },
    @{ title = "order-service"; command = "$mvnCommand -f services/order-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8086/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $EnableCheckout },
    @{ title = "payment-service"; command = "$mvnCommand -f services/payment-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8087/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $EnableCheckout },
    @{ title = "review-service"; command = "$mvnCommand -f services/review-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8088/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $EnableReviews },
    @{ title = "notification-service"; command = "$mvnCommand -f services/notification-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8090/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $EnableNotifications },
    @{ title = "api-gateway"; command = "$mvnCommand -f api-gateway/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8080/actuator/health"; healthWaitSeconds = 180; healthAttempts = 3; enabled = $true },
    @{ title = "search-service"; command = "$mvnCommand -f services/search-service/pom.xml '-Dspring-boot.run.fork=false' '-Dspring-boot.run.profiles=dev' spring-boot:run"; healthUrl = "http://127.0.0.1:8089/actuator/health"; healthWaitSeconds = 240; healthAttempts = 5; enabled = $EnableSearch }
  )

  return @($allServices | Where-Object { $_.enabled })
}

function Stop-ProjectProcesses([string]$backendDirPath, [string]$processMapPath, [switch]$StopInfraServices = $true) {
  $jobNames = @(
    "auth-service", "user-service", "product-service", "inventory-service", "cart-service",
    "order-service", "payment-service", "review-service", "search-service", "notification-service",
    "api-gateway", "ecom-storefront"
  )

  $jobs = Get-Job -ErrorAction SilentlyContinue | Where-Object { $jobNames -contains $_.Name }
  if ($jobs) {
    $jobs | Stop-Job -ErrorAction SilentlyContinue
    $jobs | Remove-Job -Force -ErrorAction SilentlyContinue
  }

  $map = Load-ProcessMap $processMapPath
  if ($map.Count -gt 0) {
    foreach ($name in @($map.Keys)) {
      Stop-TrackedProcess $name $map
    }
    Save-ProcessMap $processMapPath $map
  }

  # Kill any stale listeners from previously forked runs.
  $portsToFree = @(3000, 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090)
  $pidsToStop = New-Object System.Collections.Generic.HashSet[int]
  foreach ($port in $portsToFree) {
    $matches = netstat -ano -p tcp | Select-String -Pattern "^\s*TCP\s+\S+:$port\s+\S+\s+LISTENING\s+(\d+)\s*$"
    foreach ($match in $matches) {
      $pidValue = [int]$match.Matches[0].Groups[1].Value
      if ($pidValue -gt 0) {
        $null = $pidsToStop.Add($pidValue)
      }
    }
  }
  foreach ($pidValue in $pidsToStop) {
    try {
      Stop-Process -Id $pidValue -Force -ErrorAction Stop
    } catch {
      Write-Warning "Unable to stop stale listener PID ${pidValue}: $($_.Exception.Message)"
    }
  }

  if ($StopInfraServices -and (Test-CommandAvailable "docker")) {
    try {
      docker compose -f "$backendDirPath/infrastructure/docker-compose.yml" down | Out-Null
    } catch {
      Write-Warning "Unable to stop docker compose infra cleanly: $($_.Exception.Message)"
    }
  }
}

function Show-ProjectStatus([string]$processMapPath) {
  $targets = @(
    @{ name = "frontend"; url = "http://127.0.0.1:3000" },
    @{ name = "api-gateway"; url = "http://127.0.0.1:8080/actuator/health" },
    @{ name = "auth-service"; url = "http://127.0.0.1:8081/actuator/health" },
    @{ name = "user-service"; url = "http://127.0.0.1:8082/actuator/health" },
    @{ name = "product-service"; url = "http://127.0.0.1:8083/actuator/health" },
    @{ name = "inventory-service"; url = "http://127.0.0.1:8084/actuator/health" },
    @{ name = "cart-service"; url = "http://127.0.0.1:8085/actuator/health" },
    @{ name = "order-service"; url = "http://127.0.0.1:8086/actuator/health" },
    @{ name = "payment-service"; url = "http://127.0.0.1:8087/actuator/health" },
    @{ name = "search-service"; url = "http://127.0.0.1:8089/actuator/health" }
  )

  foreach ($target in $targets) {
    $ok = Test-HttpOk $target.url
    $status = if ($ok) { "UP" } else { "DOWN" }
    Write-Host ("{0,-14} {1,-4} {2}" -f $target.name, $status, $target.url)
  }

  $jobs = Get-Job -ErrorAction SilentlyContinue
  if ($jobs) {
    Write-Host ""
    Write-Host "PowerShell jobs:"
    $jobs | Select-Object Name, State | Format-Table -AutoSize
  }

  $map = Load-ProcessMap $processMapPath
  if ($map.Count -gt 0) {
    Write-Host ""
    Write-Host "Tracked background processes:"
    foreach ($name in $map.Keys | Sort-Object) {
      $processId = [int]$map[$name]
      $state = if (Test-ProcessAlive $processId) { "RUNNING" } else { "EXITED" }
      Write-Host ("{0,-20} PID={1,-8} {2}" -f $name, $processId, $state)
    }
  }
}

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $repoRoot "ecom-back"
$frontendDir = Join-Path $repoRoot "ecom-storefront"
$buildArtifactsDir = Join-Path $repoRoot "build-artifacts"
$processMapPath = Join-Path $buildArtifactsDir "runtime-processes.json"
if (-not (Test-Path $buildArtifactsDir)) {
  New-Item -Path $buildArtifactsDir -ItemType Directory | Out-Null
}
Cleanup-OldLogFiles -directory $buildArtifactsDir -retentionDays $LogRetentionDays

$envFilesToLoad = @()
if ([string]::IsNullOrWhiteSpace($EnvFile)) {
  $envFilesToLoad = @(
    (Join-Path $repoRoot ".env.local"),
    (Join-Path $repoRoot ".env"),
    (Join-Path $backendDir ".env"),
    (Join-Path $backendDir "services\product-service\.env"),
    (Join-Path $backendDir "services\product-service\.env.local")
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

if ($Action -eq "stop") {
  Stop-ProjectProcesses -backendDirPath $backendDir -processMapPath $processMapPath -StopInfraServices:$StopInfra
  Write-Host "Project stopped."
  return
}

if ($Action -eq "status") {
  Show-ProjectStatus -processMapPath $processMapPath
  return
}

if ($Action -eq "restart") {
  Stop-ProjectProcesses -backendDirPath $backendDir -processMapPath $processMapPath -StopInfraServices:$StopInfra
  Start-Sleep -Seconds 2
}

if ($Action -eq "start" -and (Test-AnyProjectPortListening)) {
  throw "Project ports are already in use. Use -Action status to inspect, or -Action restart for a clean restart."
}

if (-not $SkipPreflight) {
  Invoke-PreflightChecks
}

if ($StartInfra) {
  if (-not (Test-CommandAvailable "docker")) {
    throw "Docker is not installed or not available in PATH."
  }
  Write-Host "Starting infrastructure..."
  $infraServices = Get-InfraServices
  Write-Host ("Infra profile: " + ($infraServices -join ", "))
  $optionalInfraServices = @()
  if (-not $EnableSearch) {
    $optionalInfraServices += "solr"
  }
  if (-not $EnableRedis) {
    $optionalInfraServices += "redis"
  }
  if (-not $EnableKafkaUi) {
    $optionalInfraServices += "kafka-ui"
  }
  if (-not $EnableObservability) {
    $optionalInfraServices += @("zipkin", "alertmanager", "prometheus", "grafana")
  }
  try {
    docker compose -f "$backendDir/infrastructure/docker-compose.yml" up -d $infraServices
    if ($LASTEXITCODE -ne 0) {
      throw "docker compose up returned exit code $LASTEXITCODE"
    }
  } catch {
    Write-Warning "docker compose up failed once ($($_.Exception.Message)). Retrying with --remove-orphans..."
    docker compose -f "$backendDir/infrastructure/docker-compose.yml" up -d --remove-orphans $infraServices
    if ($LASTEXITCODE -ne 0) {
      throw "docker compose up failed after retry."
    }
  }
  if ($optionalInfraServices.Count -gt 0) {
    docker compose -f "$backendDir/infrastructure/docker-compose.yml" stop $optionalInfraServices 1>$null 2>$null
  }
}

if ($StartBackend) {
  Write-Host "Waiting for core infra dependencies..."
  Wait-ForInfraDependency "MySQL" "127.0.0.1" 3306 150
  Wait-ForMySqlReady "ecom-mysql" 180
  if ($EnableRedis) {
    Wait-ForInfraDependency "Redis" "127.0.0.1" 6379 120
  }
  Wait-ForInfraDependency "Kafka" "127.0.0.1" 9092 150
  if ([string]::IsNullOrWhiteSpace($env:PRODUCT_MONGODB_URI)) {
    Wait-ForInfraDependency "MongoDB" "127.0.0.1" 27018 120
  } else {
    Write-Host "Using external PRODUCT_MONGODB_URI for product-service; skipping Docker MongoDB startup/wait."
  }
  if ($EnableSearch) {
    Ensure-SolrHealthy "$backendDir/infrastructure/docker-compose.yml"
  }

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

    $serviceDefinitions = Get-BackendServiceDefinitions $mvnCommand
    Write-Host ("Backend profile: " + (($serviceDefinitions | ForEach-Object { $_.title }) -join ", "))

    foreach ($service in $serviceDefinitions) {
      if ($UseDetachedProcesses) {
        $serviceName = $service.title -replace "-service$", ""
        $serviceLog = Join-Path $buildArtifactsDir "$serviceName-startup.log"
        $serviceErr = Join-Path $buildArtifactsDir "$serviceName-startup.err.log"
        Start-DetachedCommand $service.title $backendDir $service.command $serviceLog $serviceErr $processMapPath
      } elseif ($UseJobs) {
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

    if ($UseJobs -or $UseDetachedProcesses) {
      $coreServices = @($serviceDefinitions | ForEach-Object { $_.title })

      foreach ($service in $serviceDefinitions | Where-Object { $coreServices -contains $_.title }) {
        $serviceName = $service.title -replace "-service$", ""
        $serviceLog = Join-Path $buildArtifactsDir "$serviceName-startup.log"
        $serviceErr = Join-Path $buildArtifactsDir "$serviceName-startup.err.log"
        $ok = Ensure-ServiceHealthyWithRestart `
          -name $service.title `
          -url $service.healthUrl `
          -workingDir $backendDir `
          -command $service.command `
          -logPath $serviceLog `
          -errPath $serviceErr `
          -UseDetached:$UseDetachedProcesses `
          -ProcessMapPath $processMapPath `
          -maxAttempts $service.healthAttempts `
          -waitSecondsPerAttempt $service.healthWaitSeconds
        if (-not $ok) {
          throw "$($service.title) is unhealthy after retries. See $serviceErr"
        }
      }
    }
  }
}

if ($StartFrontend) {
  if (-not (Test-CommandAvailable "npm")) {
    throw "npm is not installed or not available in PATH."
  }
  if ($CleanFrontendCache -and (Test-Path "$frontendDir/.next")) {
    Remove-Item -Path "$frontendDir/.next" -Recurse -Force -ErrorAction SilentlyContinue
  }
  if (-not (Test-Path "$frontendDir/node_modules")) {
    Write-Host "Installing frontend dependencies..."
    npm --prefix "$frontendDir" install
  }

  if ($UseDetachedProcesses) {
    $frontLog = Join-Path $buildArtifactsDir "storefront-startup.log"
    $frontErr = Join-Path $buildArtifactsDir "storefront-startup.err.log"
    Start-DetachedCommand "ecom-storefront" $frontendDir "npm run dev" $frontLog $frontErr $processMapPath
    $frontOk = Ensure-ServiceHealthyWithRestart `
      -name "ecom-storefront" `
      -url "http://127.0.0.1:3000" `
      -workingDir $frontendDir `
      -command "npm run dev" `
      -logPath $frontLog `
      -errPath $frontErr `
      -UseDetached:$true `
      -ProcessMapPath $processMapPath `
      -maxAttempts 2 `
      -waitSecondsPerAttempt 180
    if (-not $frontOk) {
      Write-Warning "Storefront unhealthy after retry. Running npm install and retrying once..."
      npm --prefix "$frontendDir" install
      Start-DetachedCommand "ecom-storefront" $frontendDir "npm run dev" $frontLog $frontErr $processMapPath
      if (-not (Wait-ForServiceHealth "ecom-storefront" "http://127.0.0.1:3000" 180)) {
        throw "ecom-storefront is still unhealthy. See $frontErr"
      }
    }
  } elseif ($UseJobs) {
    $frontLog = Join-Path $buildArtifactsDir "storefront-startup.log"
    $frontErr = Join-Path $buildArtifactsDir "storefront-startup.err.log"
    Start-JobCommand "ecom-storefront" $frontendDir "npm run dev" $frontLog $frontErr
    $frontOk = Ensure-ServiceHealthyWithRestart `
      -name "ecom-storefront" `
      -url "http://127.0.0.1:3000" `
      -workingDir $frontendDir `
      -command "npm run dev" `
      -logPath $frontLog `
      -errPath $frontErr `
      -UseDetached:$false `
      -maxAttempts 2 `
      -waitSecondsPerAttempt 180
    if (-not $frontOk) {
      Write-Warning "Storefront unhealthy after retry. Running npm install and retrying once..."
      npm --prefix "$frontendDir" install
      Start-JobCommand "ecom-storefront" $frontendDir "npm run dev" $frontLog $frontErr
      if (-not (Wait-ForServiceHealth "ecom-storefront" "http://127.0.0.1:3000" 180)) {
        throw "ecom-storefront is still unhealthy. See $frontErr"
      }
    }
  } else {
    Start-TerminalCommand "ecom-storefront" $frontendDir "npm run dev"
  }
}

Write-Host "Side-by-side dev startup triggered."
if ($UseDetachedProcesses) {
  Write-Host ""
  Write-Host "Running in detached background process mode."
  Write-Host "Status: .\run-side-by-side.ps1 -Action status"
  Write-Host "Stop all: .\run-side-by-side.ps1 -Action stop"
} elseif ($UseJobs) {
  Write-Host ""
  Write-Host "Running in integrated terminal mode (PowerShell jobs)."
  Write-Host "Check jobs: Get-Job"
  Write-Host "Stream logs: Receive-Job -Name <job-name> -Keep"
  Write-Host "Stop all: Get-Job | Stop-Job; Get-Job | Remove-Job"
}
