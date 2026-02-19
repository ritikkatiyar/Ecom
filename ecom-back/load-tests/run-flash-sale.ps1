param(
  [string]$BaseUrl = "http://localhost:8084",
  [int]$Vus = 200,
  [int]$Iterations = 10000,
  [int]$StockQty = 10000,
  [string]$MaxDuration = "2m",
  [double]$ReserveSuccessTarget = 0.95
)

$ErrorActionPreference = "Stop"

function Resolve-K6Command {
  $k6 = Get-Command k6 -ErrorAction SilentlyContinue
  if ($k6) { return "k6" }
  throw "k6 is not installed. Install k6 and re-run this script."
}

$k6Command = Resolve-K6Command
$scriptPath = Join-Path $PSScriptRoot "k6/flash-sale-inventory.js"

$env:BASE_URL = $BaseUrl
$env:VUS = "$Vus"
$env:ITERATIONS = "$Iterations"
$env:STOCK_QTY = "$StockQty"
$env:MAX_DURATION = $MaxDuration
$env:RESERVE_SUCCESS_TARGET = "$ReserveSuccessTarget"

Write-Host "Running flash-sale load test against $BaseUrl"
Write-Host "VUs=$Vus Iterations=$Iterations StockQty=$StockQty SuccessTarget=$ReserveSuccessTarget"

& $k6Command run $scriptPath
