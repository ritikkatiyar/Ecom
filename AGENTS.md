# AGENTS.md

## Cursor Cloud specific instructions

### Architecture Overview
This is an e-commerce platform ("Anaya Candles") with a Java/Spring Boot microservices backend and a Next.js storefront frontend. See `ecom-back/README.md` and `ecom-storefront/README.md` for standard commands.

### Infrastructure (Docker Compose)
Start infrastructure services before any backend service:
```bash
docker compose -f ecom-back/infrastructure/docker-compose.yml up -d
```
Required containers: MySQL (3306), MongoDB (27017), Redis (6379), Kafka (9092), Zookeeper, Elasticsearch (9200). Optional: Zipkin, Prometheus, Grafana, Alertmanager (alertmanager exits on startup due to missing env config — this is expected and non-blocking).

**Gotcha:** Elasticsearch (8.16.1) is memory-hungry and may get OOM-killed (exit code 137) in constrained environments. Restart it with `docker restart ecom-es` if this happens. The search-service health check will report DOWN until ES recovers.

### Backend Services
Build all modules first: `cd ecom-back && mvn -q -DskipTests clean install`

Start individual services with: `mvn -f services/<service-name>/pom.xml spring-boot:run -DskipTests`

Key services and ports:
| Service | Port | Command |
|---------|------|---------|
| API Gateway | 8080 | `mvn -f api-gateway/pom.xml spring-boot:run -DskipTests` |
| Auth Service | 8081 | `mvn -f services/auth-service/pom.xml spring-boot:run -DskipTests` |
| Product Service | 8083 | `mvn -f services/product-service/pom.xml spring-boot:run -DskipTests` |
| Cart Service | 8085 | `mvn -f services/cart-service/pom.xml spring-boot:run -DskipTests` |
| Inventory Service | 8084 | `mvn -f services/inventory-service/pom.xml spring-boot:run -DskipTests` |
| Search Service | 8089 | `mvn -f services/search-service/pom.xml spring-boot:run -DskipTests` |

All commands run from `ecom-back/` directory.

### Frontend
```bash
cd ecom-storefront && npm install && npm run dev
```
Runs on port 3000. Proxies `/api/*` to the API Gateway at `localhost:8080`.

### Testing Gotchas

- **JUnit 5 tests won't run with default surefire:** The parent POM doesn't configure `maven-surefire-plugin` and the bundled version (2.12.4) doesn't support JUnit Jupiter. Use the explicit plugin invocation: `mvn org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test -f <module>/pom.xml`
- **Inventory integration test is very slow:** `InventorySagaConsumerIntegrationTest` can take 25+ minutes due to Kafka consumer polling timeouts.
- **ESLint/lint is broken:** `eslint.config.mjs` imports `eslint-config-next/core-web-vitals` without `.js` extension, which fails with ESM resolution in the current `eslint-config-next` version (15.5.12). This is a pre-existing issue.

### Known Pre-existing Issues
- Product service API returns 400 on list endpoints due to missing `-parameters` compiler flag in `maven-compiler-plugin` configuration. Spring can't resolve `@RequestParam` names without it.
- The `run-side-by-side.ps1` and `.vscode/tasks.json` reference the deprecated `ecom-frontend` (Vite), not the current `ecom-storefront` (Next.js).
