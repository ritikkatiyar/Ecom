# Service Status Tracker

Last updated: 2026-02-14

## Overall
- Backend Phase 2 progress: `~89%`
- Production hardening maturity: `~40%`

## Status Legend
- `Done`: Core service is feature-complete for current phase.
- `In Progress`: Significant functionality exists but hardening/edge flows remain.
- `Not Started`: Scaffold exists but domain implementation is pending.

## Services

| Service | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| API Gateway | 70% | In Progress | Added global JWT validation, correlation ID propagation, API-version enforcement, and in-memory rate limiting. | Move to distributed rate limiting + standardized gateway error responses. |
| Auth Service | 80% | In Progress | JWT + refresh + blacklist + OAuth2 baseline implemented. | Token rotation hardening, audits, integration tests, gateway policy hookup. |
| User Service | 10% | Not Started | Scaffold + health only. | Build profile/address/preferences APIs on MySQL. |
| Product Service | 70% | In Progress | CRUD + pagination/filter/sort implemented on MongoDB. | Variants depth, indexing events, stricter validation/versioning. |
| Inventory Service | 83% | In Progress | Saga consumers now include consumer dedup and outbox-based publishing for inventory outcome events. | Reservation expiry scheduler and stronger concurrency/contract tests. |
| Cart Service | 70% | In Progress | Guest Redis + user MySQL cart + merge implemented. | Price snapshot/validation + cart eventing. |
| Order Service | 80% | In Progress | Outbox publishing + payment event handling + inventory reservation failure compensation consumer implemented. | Failure replay tooling and broader saga observability. |
| Payment Service | 70% | In Progress | Intent/webhook/idempotency + consumer dedup + outbox-based payment result publishing implemented. | Provider integration hardening, signature verification, retries/DLQ. |
| Review Service | 10% | Not Started | Scaffold + health only. | Implement rating/review CRUD + moderation model. |
| Search Service | 78% | In Progress | Ranking/reindex hardening plus consumer dedup for product indexing events. | Final relevance calibration + contract/integration tests. |
| Notification Service | 80% | In Progress | SMTP/log provider options, DLQ persistence/publish/requeue, and consumer dedup now implemented. | Production provider credentials + template engine + DLQ monitoring. |

## Platform and Cross-Cutting

| Area | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| Docker Compose Infra | 70% | In Progress | MySQL/MongoDB/Redis/Kafka/ES/Zipkin/Prometheus/Grafana are provisioned. | Add stronger healthchecks/init scripts and persistent tuning. |
| OpenAPI Coverage | 70% | In Progress | Springdoc wired in active services. | Align contracts and standardize response/error schemas. |
| Lombok Adoption | 70% | In Progress | Core entities/models migrated in active services. | Complete remaining classes and standardize style. |
| SOLID (DIP) Structure | 60% | In Progress | Controllers/consumers now depend on service interfaces in active modules. | SRP split large services and formalize orchestration patterns. |
| Kafka Contracts | 60% | In Progress | Order/payment/search plus inventory reservation outcome events and saga consumers are wired. | Schema versioning, DLQ strategy, contract tests. |
| Outbox + Global Idempotency | 75% | In Progress | Outbox producer pattern in order/payment/inventory; consumer dedup in order/payment/inventory/search/notification; webhook and event idempotency flows are active. | Add cleanup/replay tooling and standardized shared library extraction. |
| Observability in App Code | 30% | In Progress | Infra exists; basic request correlation propagation at gateway is now added. | Add service metrics/tracing/log correlation dashboards and alerts. |
| CI/CD | 10% | Not Started | Basic local workflow only. | Add build/test/quality/deploy pipelines. |
| Load Testing | 5% | Not Started | No scenarios yet. | Add k6 suites for browse/cart/checkout/flash-sale paths. |

## Immediate Execution Order
1. Add saga replay/timeout monitoring tooling.
2. Gateway distributed rate limiter + policy tuning.
3. Search relevance calibration with test dataset.
4. Notification template engine + alerting integration.
5. Shared outbox/dedup library extraction across services.

