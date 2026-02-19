param(
  [ValidateSet("browse","cart","checkout","all")]
  [string]$Suite = "all",
  [string]$ApiVersion = "v1"
)

$ErrorActionPreference = "Stop"

function Resolve-K6Command {
  $k6 = Get-Command k6 -ErrorAction SilentlyContinue
  if ($k6) { return "k6" }
  throw "k6 is not installed. Install k6 and re-run this script."
}

function Run-Suite([string]$scriptPath, [hashtable]$envMap) {
  foreach ($pair in $envMap.GetEnumerator()) {
    $env:$($pair.Key) = "$($pair.Value)"
  }
  & $k6Command run $scriptPath
}

$k6Command = Resolve-K6Command
$k6Dir = Join-Path $PSScriptRoot "k6"

if ($Suite -eq "browse" -or $Suite -eq "all") {
  Write-Host "Running browse baseline..."
  Run-Suite (Join-Path $k6Dir "browse-products.js") @{
    BASE_URL = "http://localhost:8083"
    API_VERSION = $ApiVersion
    VUS = 120
    ITERATIONS = 6000
    MAX_DURATION = "90s"
  }
}

if ($Suite -eq "cart" -or $Suite -eq "all") {
  Write-Host "Running cart baseline..."
  Run-Suite (Join-Path $k6Dir "cart-operations.js") @{
    BASE_URL = "http://localhost:8085"
    API_VERSION = $ApiVersion
    VUS = 100
    ITERATIONS = 5000
    MAX_DURATION = "90s"
  }
}

if ($Suite -eq "checkout" -or $Suite -eq "all") {
  Write-Host "Running checkout baseline..."
  Run-Suite (Join-Path $k6Dir "checkout-flow.js") @{
    ORDER_BASE_URL = "http://localhost:8086"
    PAYMENT_BASE_URL = "http://localhost:8087"
    API_VERSION = $ApiVersion
    VUS = 80
    ITERATIONS = 2500
    MAX_DURATION = "120s"
    INCLUDE_PAYMENT_INTENT = "true"
  }
}
