# Service Status Tracker

Last updated: 2026-02-14

## Overall
- Backend Phase 2 progress: `~65%`
- Production hardening maturity: `~40%`

## Status Legend
- `Done`: Core service is feature-complete for current phase.
- `In Progress`: Significant functionality exists but hardening/edge flows remain.
- `Not Started`: Scaffold exists but domain implementation is pending.

## Services

| Service | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| API Gateway | 35% | In Progress | Route forwarding baseline configured. | Add JWT enforcement, correlation IDs, rate limiting, API versioning. |
| Auth Service | 80% | In Progress | JWT + refresh + blacklist + OAuth2 baseline implemented. | Token rotation hardening, audits, integration tests, gateway policy hookup. |
| User Service | 10% | Not Started | Scaffold + health only. | Build profile/address/preferences APIs on MySQL. |
| Product Service | 70% | In Progress | CRUD + pagination/filter/sort implemented on MongoDB. | Variants depth, indexing events, stricter validation/versioning. |
| Inventory Service | 70% | In Progress | Stock + reserve/release/confirm with MySQL + Redis lock. | Reservation expiry job, inventory events, stronger concurrency tests. |
| Cart Service | 70% | In Progress | Guest Redis + user MySQL cart + merge implemented. | Price snapshot/validation + cart eventing. |
| Order Service | 65% | In Progress | Order lifecycle baseline + payment event consumer + order-created producer. | Saga compensation completeness + outbox/idempotency. |
| Payment Service | 60% | In Progress | Intent + webhook + idempotency baseline + Kafka producer/consumer. | Provider integration hardening, signature verification, retries/DLQ. |
| Review Service | 10% | Not Started | Scaffold + health only. | Implement rating/review CRUD + moderation model. |
| Search Service | 10% | Not Started | Scaffold + health only. | Elasticsearch indexing + fuzzy/autocomplete/ranking APIs. |
| Notification Service | 10% | Not Started | Scaffold + health only. | Kafka consumers + email template + retry/DLQ policy. |

## Platform and Cross-Cutting

| Area | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| Docker Compose Infra | 70% | In Progress | MySQL/MongoDB/Redis/Kafka/ES/Zipkin/Prometheus/Grafana are provisioned. | Add stronger healthchecks/init scripts and persistent tuning. |
| OpenAPI Coverage | 70% | In Progress | Springdoc wired in active services. | Align contracts and standardize response/error schemas. |
| Lombok Adoption | 70% | In Progress | Core entities/models migrated in active services. | Complete remaining classes and standardize style. |
| SOLID (DIP) Structure | 60% | In Progress | Controllers/consumers now depend on service interfaces in active modules. | SRP split large services and formalize orchestration patterns. |
| Kafka Contracts | 45% | In Progress | Baseline order/payment events exist. | Schema versioning, DLQ strategy, contract tests. |
| Outbox + Global Idempotency | 20% | In Progress | Partial idempotency present in payment/webhook flow. | Implement outbox pattern + consumer dedup across services. |
| Observability in App Code | 25% | In Progress | Infra exists; code instrumentation is limited. | Add service metrics/tracing/log correlation dashboards and alerts. |
| CI/CD | 10% | Not Started | Basic local workflow only. | Add build/test/quality/deploy pipelines. |
| Load Testing | 5% | Not Started | No scenarios yet. | Add k6 suites for browse/cart/checkout/flash-sale paths. |

## Immediate Execution Order
1. Search Service implementation.
2. Notification Service implementation.
3. Outbox + idempotent consumer foundation.
4. Saga compensation hardening (order/inventory/payment).
5. Gateway security and platform hardening.

