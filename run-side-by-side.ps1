param(
  [switch]$StartInfra = $true,
  [switch]$StartFrontend = $true,
  [switch]$StartBackend = $true
)

$ErrorActionPreference = "Stop"

function Start-TerminalCommand([string]$title, [string]$workingDir, [string]$command) {
  $full = "Set-Location '$workingDir'; $command"
  Start-Process powershell -ArgumentList "-NoExit", "-Command", "$host.ui.RawUI.WindowTitle='$title'; $full"
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

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$backendDir = Join-Path $repoRoot "ecom-back"
$frontendDir = Join-Path $repoRoot "ecom-frontend"

if ($StartInfra) {
  Write-Host "Starting infrastructure..."
  docker compose -f "$backendDir/infrastructure/docker-compose.yml" up -d
}

if ($StartBackend) {
  $mvnCommand = Resolve-MavenCommand
  if (-not $mvnCommand) {
    Write-Warning "Maven (mvn) is not installed. Install Maven, then rerun this script."
  } else {
    Write-Host "Installing shared modules..."
    Push-Location $backendDir
    try {
      Invoke-Expression "$mvnCommand -pl common/common-core,common/common-events,common/common-security -am -DskipTests install"
      if ($LASTEXITCODE -ne 0) {
        throw "Shared module installation failed."
      }
    } finally {
      Pop-Location
    }

    Start-TerminalCommand "auth-service" $backendDir "$mvnCommand -f services/auth-service/pom.xml spring-boot:run"
    Start-TerminalCommand "product-service" $backendDir "$mvnCommand -f services/product-service/pom.xml spring-boot:run"
    Start-TerminalCommand "inventory-service" $backendDir "$mvnCommand -f services/inventory-service/pom.xml spring-boot:run"
    Start-TerminalCommand "cart-service" $backendDir "$mvnCommand -f services/cart-service/pom.xml spring-boot:run"
    Start-TerminalCommand "order-service" $backendDir "$mvnCommand -f services/order-service/pom.xml spring-boot:run"
    Start-TerminalCommand "payment-service" $backendDir "$mvnCommand -f services/payment-service/pom.xml spring-boot:run"
    Start-TerminalCommand "api-gateway" $backendDir "$mvnCommand -f api-gateway/pom.xml spring-boot:run"
  }
}

if ($StartFrontend) {
  if (-not (Test-Path "$frontendDir/node_modules")) {
    Write-Host "Installing frontend dependencies..."
    npm --prefix "$frontendDir" install
  }
  Start-TerminalCommand "ecom-frontend" $frontendDir "npm run dev"
}

Write-Host "Side-by-side dev startup triggered."
