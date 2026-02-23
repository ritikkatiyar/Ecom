# Amazon Lite Progress Summary

Generated on: 2026-02-21

## Overall Snapshot
- Backend (Phase 2 target scope): `~99% complete`
- Full backend production maturity target: `~40% complete`
- Frontend (beta release track): `~55% complete`

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
  - `common/common-redis`
  - `common/common-security`
  - `common/common-web`
- Docker infra in `ecom-back/infrastructure/docker-compose.yml` with:
  - MySQL, MongoDB, Redis
  - Kafka + Zookeeper
  - Elasticsearch
  - Zipkin, Prometheus, Grafana
- Shared reliability library extracted in `common/common-core` for outbox publish loop and consumer dedup support.

### Implemented Backend Services (Baseline Functional)
- `auth-service`
  - signup/login/refresh/logout/validate
  - JWT + refresh token + Redis blacklist (with `common-redis` fallback when Redis unavailable)
  - OAuth2 Google login handler
  - SOLID/DIP boundary via `AuthUseCases` + SRP split (`AuthTokenIssuer`, `RefreshTokenGenerator`)
- `product-service`
  - CRUD + pagination/filter/sort on MongoDB
  - Cloudinary image upload (`POST /api/products/images`, max 10MB), Product `imageUrls`, admin ProductForm image picker with 401 retry
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
  - SOLID/SRP split with `CartUseCases` + dedicated owner/store components (`CartOwnerResolver`, `UserCartStore`, `GuestCartStore`)
- `order-service`
  - create/get/list/cancel/confirm lifecycle baseline
  - emits `order.created.v1` through outbox
  - consumes payment results + inventory reservation failure events
  - timeout sweep + `order.timed-out.v1` publish + failed-outbox replay endpoint
  - SRP split with dedicated item codec, response mapper, and event publisher components
- `payment-service`
  - payment intent, webhook handling, idempotency checks
  - consumes `order.created.v1`
  - emits `payment.authorized.v1` and `payment.failed.v1` through outbox
  - webhook HMAC signature verification over raw payload
  - failure-injection tests for missing/invalid signature and malformed payload rejection
  - provider outage drill controls (`/provider/outage-mode`) with retry + DLQ for intent creation
  - provider dead-letter requeue APIs and observability counters for retry/DLQ/requeue/toggle
  - SRP split with dedicated provider allocator, result publisher, and response mapper components
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
- `user-service`
  - profile, address, and preferences APIs on MySQL
  - default-address normalization (`set default` clears prior defaults for the user)
  - DIP boundary via `UserUseCases` interface
- `review-service`
  - review/rating CRUD + moderation workflow on MySQL
  - moderation status model (`PENDING`, `APPROVED`, `REJECTED`)
  - product listing path returns `APPROVED` by default with optional pending visibility
  - DIP boundary via `ReviewUseCases` interface

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
  - cart-service now follows DIP/SRP at controller-service-store boundaries
  - auth-service follows DIP/SRP via `AuthUseCases`, `AuthTokenIssuer`, and `RefreshTokenGenerator`
  - gateway JWT guard follows SRP via `GatewayAuthRoutePolicy` and `AuthValidationClient`
- Gateway hardening baseline applied:
  - request logging on every API call (method, path, status, duration, correlation-id, client IP)
  - JWT validation via auth-service
  - correlation ID propagation
  - API version header enforcement
  - Redis-backed rate limiting with in-memory fallback when Redis unavailable
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
- Event contract governance gate added:
  - contract registry: `ecom-back/contracts/events/event-contracts.json`
  - per-event JSON schema files under `ecom-back/contracts/events/schemas/`
  - validator script: `ecom-back/scripts/check_event_contracts.py`
  - enforced in both `backend-quality.yml` and `backend-release.yml`
- Next.js storefront (`ecom-storefront`) scaffolded:
  - App Router with routes: `/`, `/shop`, `/products/[id]`, `/search`, `/cart`, `/account`, `/collections`
  - stitch design system (Voluspa style): primary `#2badee`, background `#F8F6F3`, fonts Newsreader/Inter, Material Symbols
  - API proxy via `NEXT_PUBLIC_BACKEND_URL` (rewrites `/api/*` to gateway)
  - launch docs: `LAUNCH_PLAN.md`, `RELEASE_ASAP_CHECKLIST.md`, `V1_SCOPE.md`
  - gateway runtime flags endpoint: `GET /internal/frontend-flags` (for future feature flags)
  - legacy `ecom-frontend` (Vite) deprecated; storefront is primary
- Release pipeline workflow added: `.github/workflows/backend-release.yml` with staged promotion (`quality -> package -> staging -> production`) and environment gates.
- Load test harness added: `ecom-back/load-tests/k6/flash-sale-inventory.js` with SLO thresholds (`p95`, success-rate, oversell=0) and `ecom-back/load-tests/run-flash-sale.ps1`.
- Baseline k6 suites added for browse/cart/checkout:
  - `ecom-back/load-tests/k6/browse-products.js`
  - `ecom-back/load-tests/k6/cart-operations.js`
  - `ecom-back/load-tests/k6/checkout-flow.js`
  - unified runner: `ecom-back/load-tests/run-baseline-suites.ps1`
- CI perf gate added in release pipeline:
  - `backend-release.yml` job `load-regression`
  - budget-enforced k6 runs against staging browse/cart/checkout endpoints
- Production-aware perf gate added in release pipeline:
  - `backend-release.yml` job `load-regression-production-read-heavy`
  - read-heavy browse/cart profile + conservative checkout profile with separate budgets
- Weekly production load-budget calibration automation added:
  - workflow: `.github/workflows/load-budget-calibration.yml`
  - script: `ecom-back/scripts/calibrate_load_budgets.py`
  - telemetry auto-ingestion script: `ecom-back/scripts/fetch_load_telemetry_from_prometheus.py`
  - outputs: calibration JSON/MD reports uploaded as workflow artifacts
  - Prometheus query ingestion with fallback to repo-variable observed defaults
  - configurable safety-margin factors for final recommendations
  - approved calibration deltas applied to production release workflow p95 budgets:
    - Browse: `260 -> 276`
    - Cart: `300 -> 312`
    - Checkout: `420 -> 432`
- Post-deploy release safety added: smoke check gate + rollback trigger automation in `backend-release.yml`, with runbook `ecom-back/infrastructure/runbooks/DEPLOY_SMOKE_ROLLBACK.md`.
- Release-gate visibility hardened:
  - rollback verification callbacks (`post_release_gate_callback.py`)
  - staging/production release gate summaries in `GITHUB_STEP_SUMMARY`
  - `staging-release-gate-report` and `production-release-gate-report` artifacts
  - gateway internal callback metrics ingestion endpoint (`POST /internal/release-gate/callbacks`) wired through optional `CALLBACK_METRICS_URL` in release workflow callback steps
  - calibrated release-gate Prometheus alerts (`ReleaseGateRollbackCallbackFailuresPresent`, `ReleaseGateRollbackCallbackFailureRateCritical`, `ReleaseGateRollbackCallbackDrillMissing`) and updated drill thresholds in rollback runbook
  - rollback drill-delta evaluation script added (`ecom-back/scripts/evaluate_release_gate_drills.py`) with runbook logging template for threshold change tracking
- Payment outage observability hardening added:
  - Prometheus rules for `payment_provider_retry_total`, `payment_provider_dlq_total`, and requeue failures
  - Alertmanager payment owner routing (warning/critical)
  - runbook: `ecom-back/infrastructure/runbooks/PAYMENT_OUTAGE_DRILL.md`
- Weekly production receiver drill cadence added:
  - workflow: `.github/workflows/ops-receiver-drill.yml`
  - script: `ecom-back/scripts/run_ops_receiver_drill.py`
  - synthetic warning/critical alert fire mode against Alertmanager API
  - drill artifacts for auditability (`ops-receiver-drill-report.json/.md`)
  - pre-drill production receiver validation gate: `ecom-back/scripts/validate_alertmanager_receivers.py` (placeholder/malformed config blocker + artifacted report)
- Release readiness checklist gate added:
  - workflow: `.github/workflows/release-readiness-checklist.yml`
  - checker script: `ecom-back/scripts/check_release_readiness.py`
  - enforces receiver validation + ops drill + load calibration + rollback drill delta evidence
- Scheduled release-gate drill automation added:
  - workflow: `.github/workflows/release-gate-drill.yml`
  - drill runner script: `ecom-back/scripts/run_release_gate_drill.py`
  - produces `staging-release-gate.json`, `production-release-gate.json`, and evaluated tuning delta report artifacts
  - evidence-quality gate script: `ecom-back/scripts/check_release_gate_drill_evidence.py` (fails drill workflow when staged/prod status is `missing` or `attention_required`)
- Search dataset ownership automation added:
  - rotation config: `search-relevance-dataset-ownership.json`
  - assignment script: `ecom-back/scripts/assign_search_dataset_reviewer.py`
  - weekly workflow: `.github/workflows/search-dataset-rota.yml` (cadence report + issue assignment when due)
  - reviewer-pool onboarding via `SEARCH_DATASET_REVIEWERS` repo variable override
  - due-issue auto-close policy when dataset is within cadence
  - minimum reviewer-pool policy (`SEARCH_DATASET_MIN_REVIEWERS`, default `2`) and collaborator permission validation gate for assignee eligibility
- Release-gate rollback telemetry dashboard added:
  - `ecom-back/infrastructure/grafana/dashboards/release-gate-observability.json`
  - panels for rollback callback volume/failures/status split

## Service-by-Service Completion

| Service | Completion | Status |
|---|---:|---|
| API Gateway | 97% | In Progress |
| Auth Service | 85% | In Progress |
| User Service | 58% | In Progress |
| Product Service | 75% | In Progress |
| Inventory Service | 96% | In Progress |
| Cart Service | 78% | In Progress |
| Order Service | 94% | In Progress |
| Payment Service | 93% | In Progress |
| Review Service | 56% | In Progress |
| Search Service | 93% | In Progress |
| Notification Service | 91% | In Progress |

## Major Backend Gaps Remaining
1. Complete remaining business hardening for `user-service` and `review-service` (integration tests, auth-policy coupling, and eventing decisions).
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
3. Keep calibration delta changes tied to artifact evidence and weekly signoff.
4. Complete user/review services and lock backend phase-exit criteria.
5. Increase SOLID maturity with SRP/port-adapter refactors in complex services.


