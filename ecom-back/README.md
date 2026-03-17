# Ecom Back (Phase 2 Scaffold)

This repository contains the Phase 2 backend microservices scaffold for Amazon Lite.

## Modules
- `api-gateway`
- `common/common-core`
- `common/common-events`
- `common/common-redis`
- `common/common-security`
- `common/common-web`
- `services/auth-service`
- `services/user-service`
- `services/product-service`
- `services/inventory-service`
- `services/cart-service`
- `services/order-service`
- `services/payment-service`
- `services/review-service`
- `services/search-service`
- `services/notification-service`

## Infra
- `infrastructure/docker-compose.yml` supports a lighter default dev stack: MySQL, MongoDB, Kafka in single-node KRaft mode.
- Docker MongoDB is published on host port `27018` so `product-service` does not collide with any local MongoDB already using `27017`.
- Optional on demand:
  - `-EnableRedis:$true` adds Redis.
  - `-EnableCheckout:$true` adds `order-service` and `payment-service`.
  - `-EnableReviews:$true` adds `review-service`.
  - `-EnableNotifications:$true` adds `notification-service`.
  - `-EnableKafkaUi:$true` adds Kafka UI at `http://127.0.0.1:8091`.
  - `-EnableSearch:$true` adds Elasticsearch and `search-service`.
  - `-EnableObservability:$true` adds Zipkin, Prometheus, Alertmanager, and Grafana.

## Run (local)
1. From repo root (`d:\ecom`), run one-command startup:
   - `.\run-side-by-side.ps1 -Action start`
2. If execution is blocked by policy in the current shell:
   - `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`
   - `.\run-side-by-side.ps1 -Action start`
3. Optional variants:
   - Stop all: `.\run-side-by-side.ps1 -Action stop`
   - Restart all: `.\run-side-by-side.ps1 -Action restart`
   - Status: `.\run-side-by-side.ps1 -Action status`
   - Start Redis too: `.\run-side-by-side.ps1 -Action start -EnableRedis:$true`
   - Start checkout flow: `.\run-side-by-side.ps1 -Action start -EnableCheckout:$true`
   - Start reviews: `.\run-side-by-side.ps1 -Action start -EnableReviews:$true`
   - Start notifications: `.\run-side-by-side.ps1 -Action start -EnableNotifications:$true`
   - Start Kafka UI: `.\run-side-by-side.ps1 -Action start -EnableKafkaUi:$true`
   - Start search too: `.\run-side-by-side.ps1 -Action start -EnableSearch:$true`
   - Start full stack including observability: `.\run-side-by-side.ps1 -Action start -EnableRedis:$true -EnableCheckout:$true -EnableReviews:$true -EnableNotifications:$true -EnableSearch:$true -EnableObservability:$true`
   - Skip infra on start: `.\run-side-by-side.ps1 -Action start -StartInfra:$false`
   - Skip shared-module install: `.\run-side-by-side.ps1 -Action start -SkipSharedInstall:$true`
   - Skip preflight: `.\run-side-by-side.ps1 -Action start -SkipPreflight:$true`
   - Skip Cloudinary env check: `.\run-side-by-side.ps1 -Action start -SkipCloudinaryCheck:$true`

`run-side-by-side.ps1` now includes preflight internally and fails fast when core tooling/env is missing.

Spring config follows a shared-base plus environment-override layout:
- `application.yml`: shared settings only
- `application-dev.yml`: local development overrides
- `application-prod.yml`: production overrides

Local startup uses the `dev` profile explicitly through `run-side-by-side.ps1`. Production should set `SPRING_PROFILES_ACTIVE=prod` at runtime.

This starts:
- API Gateway (`8080`)
- Auth (`8081`), User (`8082`), Product (`8083`), Inventory (`8084`)
- Cart (`8085`)
- Order (`8086`) and Payment (`8087`) only when `-EnableCheckout:$true` is used
- Review (`8088`) only when `-EnableReviews:$true` is used
- Notification (`8090`) only when `-EnableNotifications:$true` is used
- Search (`8089`) only when `-EnableSearch:$true` is used
- Storefront (`3000`)

## Next implementation tasks
1. Auth Service: JWT/refresh token flow, blacklist with Redis, OAuth2 Google login.
2. Product Service: category/brand/variant model + filter/sort/pagination APIs.
3. Inventory Service: reservation + atomic deduction + Redis lock.
4. Order Service: saga states + outbox.
5. Payment Service: Razorpay integration + webhooks + idempotency keys.
6. Search Service: Elasticsearch indexing + autocomplete + fuzzy ranking.
7. Notification Service: Kafka consumers + templated email.
