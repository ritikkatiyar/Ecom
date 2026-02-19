# Load Tests

## Flash Sale SLO Gate (Inventory)

Script:
- `k6/flash-sale-inventory.js`

Runner:
- `run-flash-sale.ps1`

### Preconditions
- Infrastructure and backend services are running.
- `inventory-service` is reachable (default: `http://localhost:8084`).
- `k6` is installed and available on `PATH`.

### Execute
```powershell
powershell -ExecutionPolicy Bypass -File ecom-back/load-tests/run-flash-sale.ps1
```

### Optional tuning
```powershell
powershell -ExecutionPolicy Bypass -File ecom-back/load-tests/run-flash-sale.ps1 `
  -BaseUrl "http://localhost:8084" `
  -Vus 300 `
  -Iterations 15000 `
  -StockQty 15000 `
  -MaxDuration "3m" `
  -ReserveSuccessTarget 0.95
```

### SLO thresholds enforced by k6
- `http_req_duration p(95) < 250ms`
- `http_req_failed rate < 5%`
- `reserve_success_rate > 95%` (configurable by `RESERVE_SUCCESS_TARGET`)
- `reserve_server_error_rate < 1%`
- `oversell_violation_rate == 0`

### Oversell guard logic
At teardown, the script validates:
- `availableQuantity >= 0`
- `reservedQuantity >= 0`
- `reservedQuantity <= seededStock`
- `availableQuantity + reservedQuantity == seededStock`

If any check fails, `oversell_violation_rate` becomes non-zero and the run fails.

## Baseline Suites (Browse / Cart / Checkout)

Scripts:
- `k6/browse-products.js`
- `k6/cart-operations.js`
- `k6/checkout-flow.js`

Runner:
- `run-baseline-suites.ps1`

### Execute all baseline suites
```powershell
powershell -ExecutionPolicy Bypass -File ecom-back/load-tests/run-baseline-suites.ps1 -Suite all
```

### Execute one suite
```powershell
powershell -ExecutionPolicy Bypass -File ecom-back/load-tests/run-baseline-suites.ps1 -Suite browse
powershell -ExecutionPolicy Bypass -File ecom-back/load-tests/run-baseline-suites.ps1 -Suite cart
powershell -ExecutionPolicy Bypass -File ecom-back/load-tests/run-baseline-suites.ps1 -Suite checkout
```

### Baseline budgets
- Browse: `p95 < 180ms`, fail rate `< 2%`
- Cart: `p95 < 220ms`, fail rate `< 3%`, cart consistency `> 99%`
- Checkout: `p95 < 320ms`, fail rate `< 5%`, checkout success `> 95%`

### CI load regression gate
`backend-release.yml` includes a `load-regression` job that runs these suites against staging endpoints.

Required GitHub secrets:
- `STAGING_BROWSE_BASE_URL`
- `STAGING_CART_BASE_URL`
- `STAGING_ORDER_BASE_URL`
- `STAGING_PAYMENT_BASE_URL`

Production read-heavy profile secrets:
- `PRODUCTION_BROWSE_BASE_URL`
- `PRODUCTION_CART_BASE_URL`
- `PRODUCTION_ORDER_BASE_URL`
- `PRODUCTION_PAYMENT_BASE_URL`

Thresholds can be tuned with env vars:
- Browse: `BROWSE_P95_MS`, `BROWSE_FAIL_RATE_MAX`
- Cart: `CART_P95_MS`, `CART_FAIL_RATE_MAX`, `CART_CONSISTENCY_MIN`
- Checkout: `CHECKOUT_P95_MS`, `CHECKOUT_FAIL_RATE_MAX`, `CHECKOUT_SUCCESS_MIN`

### Production-aware regression profile
- Workflow job: `load-regression-production-read-heavy`
- Trigger: release tags (`v*`) after production deployment gate.
- Budgets (default profile):
  - Browse: `p95 < 276ms`, fail rate `< 3%`
  - Cart: `p95 < 312ms`, fail rate `< 4%`, consistency `> 98%`
  - Checkout (conservative): `p95 < 432ms`, fail rate `< 7%`, success `> 90%`, `INCLUDE_PAYMENT_INTENT=false`

## Weekly Budget Calibration Loop

- Workflow: `.github/workflows/load-budget-calibration.yml`
- Trigger:
  - Weekly on Monday (`05:00 UTC`)
  - Manual `workflow_dispatch`
- Script: `ecom-back/scripts/calibrate_load_budgets.py`
- Telemetry ingestion script: `ecom-back/scripts/fetch_load_telemetry_from_prometheus.py`
- Artifacts:
  - `build-artifacts/load-budget-observed.env`
  - `build-artifacts/load-budget-telemetry-report.json`
  - `build-artifacts/load-budget-telemetry-report.md`
  - `build-artifacts/load-budget-calibration-report.json`
  - `build-artifacts/load-budget-calibration-report.md`

### Prometheus ingestion config
- GitHub secrets:
  - `PRODUCTION_PROMETHEUS_URL` (example: `https://prometheus.example.com`)
  - `PRODUCTION_PROMETHEUS_BEARER_TOKEN` (optional if endpoint requires auth)
- Behavior:
  - Workflow first tries to fetch observed metrics from Prometheus query API.
  - If any query returns empty/error, fallback observed defaults are used from repository variables.
  - Resolved observed values are exported into `build-artifacts/load-budget-observed.env` and loaded into calibration step.

### Calibration inputs (GitHub repository variables)
- Current thresholds:
  - `CURRENT_BROWSE_P95_MS`, `CURRENT_BROWSE_FAIL_RATE_MAX`
  - `CURRENT_CART_P95_MS`, `CURRENT_CART_FAIL_RATE_MAX`, `CURRENT_CART_CONSISTENCY_MIN`
  - `CURRENT_CHECKOUT_P95_MS`, `CURRENT_CHECKOUT_FAIL_RATE_MAX`, `CURRENT_CHECKOUT_SUCCESS_MIN`
- Observed production telemetry:
  - `OBSERVED_BROWSE_P95_MS`, `OBSERVED_BROWSE_FAIL_RATE`
  - `OBSERVED_CART_P95_MS`, `OBSERVED_CART_FAIL_RATE`, `OBSERVED_CART_CONSISTENCY`
  - `OBSERVED_CHECKOUT_P95_MS`, `OBSERVED_CHECKOUT_FAIL_RATE`, `OBSERVED_CHECKOUT_SUCCESS`
- Safety margins (optional):
  - `LOAD_BUDGET_LATENCY_MARGIN_FACTOR`
  - `LOAD_BUDGET_ERROR_MARGIN_FACTOR`
  - `LOAD_BUDGET_CONSISTENCY_MARGIN_FACTOR`
  - `LOAD_BUDGET_SUCCESS_MARGIN_FACTOR`
