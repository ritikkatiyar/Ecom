# Local API Smoke Report

- Generated at: 2026-03-22T13:33:38.969588+00:00
- Status: `FAIL`
- Passed: 10
- Failed: 5

| Check | Result | Detail |
|---|---|---|
| gateway.health | PASS | status=200, url=http://localhost:8080/actuator/health, attempt=1/4 |
| auth.health | PASS | status=200, url=http://localhost:8081/actuator/health, attempt=1/4 |
| user.health | PASS | status=200, url=http://localhost:8082/actuator/health, attempt=1/4 |
| product.health | PASS | status=200, url=http://localhost:8083/actuator/health, attempt=1/4 |
| inventory.health | PASS | status=200, url=http://localhost:8084/actuator/health, attempt=1/4 |
| cart.health | PASS | status=200, url=http://localhost:8085/actuator/health, attempt=1/4 |
| order.health | PASS | status=200, url=http://localhost:8086/actuator/health, attempt=1/4 |
| payment.health | PASS | status=200, url=http://localhost:8087/actuator/health, attempt=1/4 |
| review.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8088/actuator/health, attempt=4/4 |
| search.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8089/internal/health, attempt=4/4 |
| notification.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8090/actuator/health, attempt=4/4 |
| storefront.root | PASS | status=200, url=http://localhost:3000, attempt=1/4 |
| gateway.products.list | PASS | status=200, url=http://localhost:8080/api/products?page=0&size=1, attempt=1/4 |
| gateway.search.dataset.health | FAIL | status=503, url=http://localhost:8080/api/search/admin/relevance/dataset/health, attempt=4/4 |
| gateway.contracts | FAIL | passed=4, failed=1, total=5, exit=1 |
