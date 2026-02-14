# Ecom Back (Phase 2 Scaffold)

This repository contains the Phase 2 backend microservices scaffold for Amazon Lite.

## Modules
- `api-gateway`
- `common/common-core`
- `common/common-events`
- `common/common-security`
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
1. `docker compose -f infrastructure/docker-compose.yml up -d`
2. `mvn -q -DskipTests clean install`
3. Start each service module individually.

## Next implementation tasks
1. Auth Service: JWT/refresh token flow, blacklist with Redis, OAuth2 Google login.
2. Product Service: category/brand/variant model + filter/sort/pagination APIs.
3. Inventory Service: reservation + atomic deduction + Redis lock.
4. Order Service: saga states + outbox.
5. Payment Service: Razorpay integration + webhooks + idempotency keys.
6. Search Service: Elasticsearch indexing + autocomplete + fuzzy ranking.
7. Notification Service: Kafka consumers + templated email.
