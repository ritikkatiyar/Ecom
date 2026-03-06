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
- `infrastructure/docker-compose.yml` brings up MySQL, MongoDB, Redis, Kafka, Elasticsearch, Zipkin, Prometheus, Grafana.

## Run (local)
1. From repo root (`d:\ecom`), run one-command startup:
   - `.\run-side-by-side.ps1`
2. If execution is blocked by policy in the current shell:
   - `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`
   - `.\run-side-by-side.ps1`
3. Optional variants:
   - Skip infra: `.\run-side-by-side.ps1 -StartInfra:$false`
   - Skip shared-module install: `.\run-side-by-side.ps1 -SkipSharedInstall:$true`

This starts:
- API Gateway (`8080`)
- Auth (`8081`), User (`8082`), Product (`8083`), Inventory (`8084`)
- Cart (`8085`), Order (`8086`), Payment (`8087`), Review (`8088`)
- Search (`8089`), Notification (`8090`)
- Storefront (`3000`)

## Next implementation tasks
1. Auth Service: JWT/refresh token flow, blacklist with Redis, OAuth2 Google login.
2. Product Service: category/brand/variant model + filter/sort/pagination APIs.
3. Inventory Service: reservation + atomic deduction + Redis lock.
4. Order Service: saga states + outbox.
5. Payment Service: Razorpay integration + webhooks + idempotency keys.
6. Search Service: Elasticsearch indexing + autocomplete + fuzzy ranking.
7. Notification Service: Kafka consumers + templated email.
