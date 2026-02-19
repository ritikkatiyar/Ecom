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
