# Service Status Tracker

Last updated: 2026-02-14

## Overall
- Backend Phase 2 progress: `~99%`
- Production hardening maturity: `~40%`

## Status Legend
- `Done`: Core service is feature-complete for current phase.
- `In Progress`: Significant functionality exists but hardening/edge flows remain.
- `Not Started`: Scaffold exists but domain implementation is pending.

## Services

| Service | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| API Gateway | 92% | In Progress | Added standardized JSON error payloads, public-vs-protected JWT policy tuning (GET browse/search public), correlation ID propagation, API-version enforcement, route-level Redis rate limits/timeouts, and route-level circuit breakers with fallback endpoints. | Contract tests for edge routes and breaker-threshold tuning by traffic profile. |
| Auth Service | 80% | In Progress | JWT + refresh + blacklist + OAuth2 baseline implemented. | Token rotation hardening, audits, integration tests, gateway policy hookup. |
| User Service | 10% | Not Started | Scaffold + health only. | Build profile/address/preferences APIs on MySQL. |
| Product Service | 70% | In Progress | CRUD + pagination/filter/sort implemented on MongoDB. | Variants depth, indexing events, stricter validation/versioning. |
| Inventory Service | 89% | In Progress | Saga consumers now include consumer dedup, outbox-based publishing, compensation handling for `order.timed-out` events, and scheduled outbox/dedup cleanup jobs. | Reservation expiry scheduler and stronger concurrency/contract tests. |
| Cart Service | 70% | In Progress | Guest Redis + user MySQL cart + merge implemented. | Price snapshot/validation + cart eventing. |
| Order Service | 92% | In Progress | Added timeout sweep for stale payment-pending orders, timeout event publishing, admin replay endpoint for failed outbox events, saga timeout/outbox-failure metrics, and scheduled outbox/dedup cleanup jobs. | Alert tuning and runbook-driven on-call drills. |
| Payment Service | 75% | In Progress | Intent/webhook/idempotency + consumer dedup + outbox-based payment result publishing implemented, with scheduled outbox/dedup cleanup jobs. | Provider integration hardening, signature verification, retries/DLQ. |
| Review Service | 10% | Not Started | Scaffold + health only. | Implement rating/review CRUD + moderation model. |
| Search Service | 89% | In Progress | Ranking/reindex hardening plus consumer dedup, expanded relevance dataset, target pass-rate evaluation (`meetsTarget`), and scheduled dedup cleanup added. | Finalize contract/integration tests and dataset refresh cadence. |
| Notification Service | 91% | In Progress | SMTP/log provider options, template engine, DLQ persistence/publish/requeue, alert topic publish, consumer dedup, metrics counters, and scheduled dedup cleanup implemented; Prometheus/Grafana thresholds tuned. | Production provider credentials + runbook validation under failure drills. |

## Platform and Cross-Cutting

| Area | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| Docker Compose Infra | 70% | In Progress | MySQL/MongoDB/Redis/Kafka/ES/Zipkin/Prometheus/Grafana are provisioned. | Add stronger healthchecks/init scripts and persistent tuning. |
| OpenAPI Coverage | 70% | In Progress | Springdoc wired in active services. | Align contracts and standardize response/error schemas. |
| Lombok Adoption | 70% | In Progress | Core entities/models migrated in active services. | Complete remaining classes and standardize style. |
| SOLID (DIP) Structure | 60% | In Progress | Controllers/consumers now depend on service interfaces in active modules. | SRP split large services and formalize orchestration patterns. |
| Kafka Contracts | 60% | In Progress | Order/payment/search plus inventory reservation outcome events and saga consumers are wired. | Schema versioning, DLQ strategy, contract tests. |
| Outbox + Global Idempotency | 96% | In Progress | Shared reliability library extracted in common-core and adopted by order/payment/inventory/search/notification dedup/outbox flows, with scheduled cleanup automation added. | Optional adapter consolidation and retention tuning per environment. |
| Observability in App Code | 62% | In Progress | Added Prometheus registry and Zipkin tracing baseline across gateway/auth/product/cart/inventory/order/payment/search/notification with trace-log correlation pattern, plus existing saga/notification alert tuning. | Wire Alertmanager/on-call runbooks and add business KPI dashboards per service. |
| CI/CD | 10% | Not Started | Basic local workflow only. | Add build/test/quality/deploy pipelines. |
| Load Testing | 5% | Not Started | No scenarios yet. | Add k6 suites for browse/cart/checkout/flash-sale paths. |

## Immediate Execution Order
1. Search contract/integration tests + dataset refresh workflow.
2. Alertmanager receiver wiring + runbook drill automation.
3. Reservation expiry scheduler and concurrency contract tests (inventory).
4. Edge-route contract tests (gateway auth/public/circuit-breaker paths).
5. CI pipeline quality gates for reliability and relevance checks.

