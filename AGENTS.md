# AGENTS.md

## Cursor Cloud specific instructions

### Codebase overview

This is a monorepo for "Anaya Candles" — an e-commerce platform with:
- **ecom-storefront**: Next.js 15 (App Router, TypeScript, Tailwind CSS v4) — port 3000
- **ecom-back**: Java 21 / Spring Boot 3.4.2 multi-module Maven backend with 10 microservices + API gateway
- See `ecom-back/README.md` and `ecom-storefront/README.md` for standard commands.

### Starting infrastructure

All infrastructure runs via Docker Compose:
```
sudo dockerd &>/tmp/dockerd.log &
sleep 5
sudo docker compose -f ecom-back/infrastructure/docker-compose.yml up -d mysql mongodb redis zookeeper kafka elasticsearch
```
Wait ~15s for services to be ready (MySQL, Kafka, and Elasticsearch are slowest).

### Building the backend

The project does NOT use spring-boot-starter-parent, so the `-parameters` compiler flag is not set by default. Always build with:
```
cd ecom-back && mvn -q -DskipTests clean install -Dmaven.compiler.parameters=true
```
Without `-Dmaven.compiler.parameters=true`, REST controllers using `@RequestParam` without explicit `value` will fail at runtime.

### Starting services

Start each service from `ecom-back/` directory:
```
mvn -pl api-gateway spring-boot:run -DskipTests -Dmaven.compiler.parameters=true
mvn -pl services/auth-service spring-boot:run -DskipTests -Dmaven.compiler.parameters=true
mvn -pl services/user-service spring-boot:run -DskipTests -Dmaven.compiler.parameters=true
mvn -pl services/product-service spring-boot:run -DskipTests -Dmaven.compiler.parameters=true
```

Service port map: Gateway=8080, Auth=8081, User=8082, Product=8083, Inventory=8084, Cart=8085, Order=8086, Payment=8087, Review=8088, Search=8089, Notification=8090.

### Frontend

```
cd ecom-storefront && npm run dev
```
Frontend proxies `/api/*` to `http://localhost:8080` (API Gateway). The API Gateway requires the `X-API-Version: v1` header on all requests.

### Known issues

- **ESLint**: `eslint.config.mjs` imports `eslint-config-next/core-web-vitals` without `.js` extension, causing ESM resolution failure. `npm run lint` / `npx eslint .` will fail. Pre-existing issue.
- **TypeScript**: One pre-existing type error in `lib/apiClient.ts` (TS7053). `npx tsc --noEmit` reports 1 error.
- **Tests**: Backend is in scaffold phase; `mvn test` passes but has 0 test cases.
- **Cloudinary / Google OAuth**: Product image upload and Google login require external credentials (env vars `CLOUDINARY_*`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`). Services start fine without them using defaults.
