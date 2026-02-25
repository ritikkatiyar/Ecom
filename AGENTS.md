# AGENTS.md

## Cursor Cloud specific instructions

### Overview

This is an e-commerce platform ("Anaya Candles") with a **Java/Spring Boot microservices backend** (`ecom-back/`) and a **Next.js 15 frontend** (`ecom-storefront/`). See `ecom-back/README.md` and `ecom-storefront/README.md` for standard commands.

### Prerequisites (installed by VM snapshot)

- **Java 21** (system OpenJDK)
- **Maven 3.8+** (system package)
- **Node.js 22** / **npm 10** (via nvm)
- **Docker + Docker Compose** (for infrastructure containers)

### Infrastructure startup

```bash
# Start required backing services (MySQL, MongoDB, Redis, Kafka+Zookeeper, Elasticsearch)
sudo dockerd &>/tmp/dockerd.log &
sleep 5
sudo docker compose -f /workspace/ecom-back/infrastructure/docker-compose.yml up -d mysql mongodb redis zookeeper kafka elasticsearch
```

- Elasticsearch is memory-hungry; start it with `-e "ES_JAVA_OPTS=-Xms256m -Xmx256m"` if it gets OOM-killed (exit code 137). In that case, remove the container and restart manually:
  ```bash
  sudo docker rm ecom-es
  sudo docker run -d --name ecom-es --network infrastructure_default -p 9200:9200 \
    -e "discovery.type=single-node" -e "xpack.security.enabled=false" \
    -e "ES_JAVA_OPTS=-Xms256m -Xmx256m" docker.elastic.co/elasticsearch/elasticsearch:8.16.1
  ```
- MySQL credentials: `root` / `root`. Databases are auto-created via `createDatabaseIfNotExist=true` in JDBC URLs.

### Building the backend

```bash
cd /workspace/ecom-back && mvn -q -DskipTests clean install
```

### Starting backend services

Each service runs via `mvn spring-boot:run` from the `ecom-back/` directory:

```bash
cd /workspace/ecom-back
nohup mvn -f services/auth-service/pom.xml spring-boot:run -q > /tmp/auth-service.log 2>&1 &
nohup mvn -f services/user-service/pom.xml spring-boot:run -q > /tmp/user-service.log 2>&1 &
nohup mvn -f services/product-service/pom.xml spring-boot:run -q > /tmp/product-service.log 2>&1 &
nohup mvn -f services/inventory-service/pom.xml spring-boot:run -q > /tmp/inventory-service.log 2>&1 &
nohup mvn -f services/cart-service/pom.xml spring-boot:run -q > /tmp/cart-service.log 2>&1 &
nohup mvn -f services/order-service/pom.xml spring-boot:run -q > /tmp/order-service.log 2>&1 &
nohup mvn -f services/payment-service/pom.xml spring-boot:run -q > /tmp/payment-service.log 2>&1 &
nohup mvn -f services/search-service/pom.xml spring-boot:run -q > /tmp/search-service.log 2>&1 &
nohup mvn -f api-gateway/pom.xml spring-boot:run -q > /tmp/api-gateway.log 2>&1 &
```

Service ports: auth=8081, user=8082, product=8083, inventory=8084, cart=8085, order=8086, payment=8087, search=8089, api-gateway=8080.

Health check: `curl http://localhost:<port>/actuator/health`

### Starting the frontend

```bash
cd /workspace/ecom-storefront && npm run dev
```

Runs on port 3000. Proxies `/api/*` to the API gateway at `localhost:8080`.

### Non-obvious gotchas

- **API Gateway requires `X-API-Version: v1` header** on all proxied requests. Without it, you get a 400 `API_VERSION_MISMATCH` error. The `auth` endpoints (`/api/auth/*`) work without it. The Next.js frontend's `apiClient` handles this automatically.
- **ESLint (`npm run lint`)** fails with a module resolution error in the current eslint-config-next/Next.js 15 setup (import path needs `.js` extension). This is a pre-existing issue in the repo, not a setup problem.
- **Backend tests** (`mvn test`) are lightweight — no integration tests requiring running infrastructure. The test phase compiles and runs unit tests only.
- Optional services (review-service on 8088, notification-service on 8090) can be started the same way but are not required for core flows.
- Prometheus/Grafana/Zipkin/Alertmanager are optional observability services; skip unless needed.
