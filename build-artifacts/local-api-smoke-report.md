# Local API Smoke Report

- Generated at: 2026-03-10T13:28:28.526082+00:00
- Status: `FAIL`
- Passed: 0
- Failed: 15

| Check | Result | Detail |
|---|---|---|
| gateway.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8080/actuator/health, attempt=4/4 |
| auth.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8081/actuator/health, attempt=4/4 |
| user.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8082/actuator/health, attempt=4/4 |
| product.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8083/actuator/health, attempt=4/4 |
| inventory.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8084/actuator/health, attempt=4/4 |
| cart.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8085/actuator/health, attempt=4/4 |
| order.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8086/actuator/health, attempt=4/4 |
| payment.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8087/actuator/health, attempt=4/4 |
| review.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8088/actuator/health, attempt=4/4 |
| search.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8089/internal/health, attempt=4/4 |
| notification.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8090/actuator/health, attempt=4/4 |
| storefront.root | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:3000, attempt=4/4 |
| gateway.products.list | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8080/api/products?page=0&size=1, attempt=4/4 |
| gateway.search.dataset.health | FAIL | URLError: <urlopen error [WinError 10061] No connection could be made because the target machine actively refused it>, url=http://localhost:8080/api/search/admin/relevance/dataset/health, attempt=4/4 |
| gateway.contracts | FAIL | passed=0, failed=5, total=5, exit=1 |
