# Gateway API Contracts Report

- Generated at: 2026-03-22T13:33:38.883885+00:00
- Base URL: `http://localhost:8080`
- Selected contracts: 5
- Passed: 4
- Failed: 1
- Skipped: 0
- Status: `FAIL`

| Contract | Result | Detail |
|---|---|---|
| gateway.version.header.enforced | PASS | status=400 |
| gateway.auth.required.orders | PASS | status=401 |
| gateway.auth.required.reviews | PASS | status=401 |
| gateway.products.list.contract | PASS | status=200 |
| gateway.search.products.contract | FAIL | status 503 not in expected [200]; missing response body fields: content, number, size, totalElements, totalPages |
