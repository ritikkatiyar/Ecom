# Amazon Lite Progress Summary

Generated on: 2026-02-19

## Overall Snapshot
- Backend (Phase 2 target scope): `~99% complete`
- Full backend production maturity target: `~40% complete`
- Frontend (final Next.js production architecture): `~20% complete`

## Completed So Far

### Architecture
- HLD available in `architecture/PHASE1_HLD.md`.
- Data strategy updated to MySQL where relational storage is needed.
- Core microservice boundaries and Kafka-driven architecture defined.

### Monorepo + Infra
- Backend monorepo scaffold created under `ecom-back`.
- Shared modules added:
  - `common/common-core`
  - `common/common-events`
  - `common/common-security`
- Docker infra in `ecom-back/infrastructure/docker-compose.yml` with:
  - MySQL, MongoDB, Redis
  - Kafka + Zookeeper
  - Elasticsearch
  - Zipkin, Prometheus, Grafana
- Shared reliability library extracted in `common/common-core` for outbox publish loop and consumer dedup support.

### Implemented Backend Services (Baseline Functional)
- `auth-service`
  - signup/login/refresh/logout/validate
  - JWT + refresh token + Redis blacklist
  - OAuth2 Google login handler
- `product-service`
  - CRUD + pagination/filter/sort on MongoDB
- `inventory-service`
  - stock upsert/get + reserve/release/confirm
  - MySQL persistence + Redis lock
  - saga consumers for order/payment events (reserve/confirm/release compensation)
  - outbox-based publish + consumer dedup for saga event reliability
  - compensation on order timeout events (`order.timed-out.v1`)
  - reservation expiry scheduler with concurrency contract tests
  - real MySQL + Kafka integration tests for contention and duplicate-event dedup behavior
  - outbox payload DDL hardened to MySQL-safe `TEXT` mapping (fresh-schema compatible)
- `cart-service`
  - guest cart (Redis), logged-in cart (MySQL), merge flow
- `order-service`
  - create/get/list/cancel/confirm lifecycle baseline
  - emits `order.created.v1` through outbox
  - consumes payment results + inventory reservation failure events
  - timeout sweep + `order.timed-out.v1` publish + failed-outbox replay endpoint
- `payment-service`
  - payment intent, webhook handling, idempotency checks
  - consumes `order.created.v1`
  - emits `payment.authorized.v1` and `payment.failed.v1` through outbox
  - webhook HMAC signature verification over raw payload
  - failure-injection tests for missing/invalid signature and malformed payload rejection
  - provider outage drill controls (`/provider/outage-mode`) with retry + DLQ for intent creation
  - provider dead-letter requeue APIs and observability counters for retry/DLQ/requeue/toggle
- `search-service`
  - fuzzy and boosted ranking search improvements
  - active-only filtering option
  - paged reindex from `product-service` via `POST /api/search/reindex/products`
  - product indexing consumer dedup
  - expanded curated relevance dataset evaluation with target pass-rate checks via `GET /api/search/admin/relevance/evaluate`
  - dataset freshness/refresh health endpoint via `GET /api/search/admin/relevance/dataset/health`
  - contract + unit tests for relevance evaluation and dataset refresh health
  - scheduled consumed-event dedup cleanup
- `notification-service`
  - provider abstraction with `log` and `smtp` options
  - dead-letter escalation and `notification.dlq.v1` publish
  - dead-letter list/requeue APIs
  - event consumer dedup
  - template-based subject/body rendering + `notification.alert.v1` alerts + metrics counters
  - tuned Prometheus alert thresholds and Grafana dashboard thresholds (dead-letter and failure-rate signals)
  - scheduled consumed-event dedup cleanup

### Reliability Cleanup Automation
- Added scheduled retention cleanup jobs for:
  - `order-service` outbox (`SENT`/`FAILED`) + consumed-event dedup table
  - `payment-service` outbox (`SENT`/`FAILED`) + consumed-event dedup table
  - `inventory-service` outbox (`SENT`/`FAILED`) + consumed-event dedup table
  - `notification-service` consumed-event dedup table
  - `search-service` consumed-event dedup index
- Retention is configurable using:
  - `app.cleanup.fixed-delay`
  - `app.cleanup.outbox-sent-retention`
  - `app.cleanup.outbox-failed-retention`
  - `app.cleanup.dedup-retention`

### Engineering Standards Applied
- OpenAPI support wired in active services.
- Lombok integrated in core model/entity classes.
- DIP refactor applied:
  - controllers/consumers depend on `UseCases` interfaces
  - concrete services implement interfaces
- Gateway hardening baseline applied:
  - JWT validation via auth-service
  - correlation ID propagation
  - API version header enforcement
  - Redis-backed distributed rate limiting
  - standardized JSON error payloads at gateway filters
  - route policy tuning (public GET browse/search, protected write flows)
  - route-level rate limit and timeout policies per domain
  - route-level circuit breakers with fallback responses (`/fallback/{service}`)
  - resilience4j circuit-breaker/time-limiter baseline for downstream services
- Observability baseline expanded across active services:
  - Prometheus registry dependency added to gateway and active microservices
  - Zipkin tracing bridge/reporter added for distributed tracing export
  - log correlation format includes `traceId` and `spanId` across service logs
  - management tracing config standardized (`management.tracing` + Zipkin endpoint)
  - Prometheus alert routing wired to Alertmanager in infrastructure
  - alert drill runbook added (`ecom-back/infrastructure/runbooks/ALERT_DRILL.md`)

### Developer Workflow
- `run-side-by-side.ps1` available for local startup.
- VS Code task-based integrated terminal startup is configured.
- Detailed per-service tracker maintained in `SERVICE_DETAILED_PROGRESS.md`.
- API-level docs are now maintained alongside each service via `API_DOCS.md`.
- CI quality workflow added: `.github/workflows/backend-quality.yml` (service tests + dataset freshness + API docs validation).
- Release pipeline workflow added: `.github/workflows/backend-release.yml` with staged promotion (`quality -> package -> staging -> production`) and environment gates.
- Load test harness added: `ecom-back/load-tests/k6/flash-sale-inventory.js` with SLO thresholds (`p95`, success-rate, oversell=0) and `ecom-back/load-tests/run-flash-sale.ps1`.
- Baseline k6 suites added for browse/cart/checkout:
  - `ecom-back/load-tests/k6/browse-products.js`
  - `ecom-back/load-tests/k6/cart-operations.js`
  - `ecom-back/load-tests/k6/checkout-flow.js`
  - unified runner: `ecom-back/load-tests/run-baseline-suites.ps1`
- Post-deploy release safety added: smoke check gate + rollback trigger automation in `backend-release.yml`, with runbook `ecom-back/infrastructure/runbooks/DEPLOY_SMOKE_ROLLBACK.md`.
- Payment outage observability hardening added:
  - Prometheus rules for `payment_provider_retry_total`, `payment_provider_dlq_total`, and requeue failures
  - Alertmanager payment owner routing (warning/critical)
  - runbook: `ecom-back/infrastructure/runbooks/PAYMENT_OUTAGE_DRILL.md`

## Service-by-Service Completion

| Service | Completion | Status |
|---|---:|---|
| API Gateway | 95% | In Progress |
| Auth Service | 80% | In Progress |
| User Service | 10% | Not Started (beyond scaffold) |
| Product Service | 70% | In Progress |
| Inventory Service | 96% | In Progress |
| Cart Service | 70% | In Progress |
| Order Service | 92% | In Progress |
| Payment Service | 90% | In Progress |
| Review Service | 10% | Not Started (beyond scaffold) |
| Search Service | 93% | In Progress |
| Notification Service | 91% | In Progress |

## Major Backend Gaps Remaining
1. Implement business logic for `user-service`, `review-service`, `search-service`, `notification-service`.
2. Add stronger event architecture:
   - schema/versioning discipline
   - outbox pattern rollout (active in order/payment/inventory with shared common-core helpers)
   - retries, DLQ, consumer idempotency store
3. Add stronger saga observability for compensations and runbookize replay/cleanup operations.
4. Harden API Gateway further (breaker-threshold tuning and policy regression coverage).
5. Add resilience patterns consistently (retry/circuit breaker/timeouts).
6. Complete observability instrumentation in code (business KPI dashboards and receiver integrations).
7. Search quality hardening (refresh ownership automation and cadence governance).

## Suggested Next Backend Milestone
1. Replace Alertmanager placeholder webhooks with production receiver routes.
2. Add deploy pipeline stages with environment promotion checks.
3. Add rollback verification callbacks and release dashboard visibility.
4. Add CI-triggered load regression stage with perf budget enforcement.
5. Execute search dataset ownership rota automation (cadence + reviewer assignment).
