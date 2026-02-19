# Service Status Tracker

Last updated: 2026-02-19

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
| API Gateway | 95% | In Progress | Added standardized JSON error payloads, public-vs-protected JWT policy tuning (GET browse/search public), correlation ID propagation, API-version enforcement, route-level Redis rate limits/timeouts, route-level circuit breakers with fallback endpoints, and edge-route contract tests. | Breaker-threshold tuning by traffic profile and production receiver/alert mapping drills. |
| Auth Service | 80% | In Progress | JWT + refresh + blacklist + OAuth2 baseline implemented. | Token rotation hardening, audits, integration tests, gateway policy hookup. |
| User Service | 10% | Not Started | Scaffold + health only. | Build profile/address/preferences APIs on MySQL. |
| Product Service | 70% | In Progress | CRUD + pagination/filter/sort implemented on MongoDB. | Variants depth, indexing events, stricter validation/versioning. |
| Inventory Service | 96% | In Progress | Saga consumers now include consumer dedup, outbox-based publishing, compensation handling for `order.timed-out` events, scheduled outbox/dedup cleanup jobs, reservation expiry scheduler, concurrency contract tests, and real MySQL+Kafka integration tests for contention/dedup scenarios. | Add flash-sale load profile assertions (p95 latency, oversell SLO) in k6. |
| Cart Service | 70% | In Progress | Guest Redis + user MySQL cart + merge implemented. | Price snapshot/validation + cart eventing. |
| Order Service | 92% | In Progress | Added timeout sweep for stale payment-pending orders, timeout event publishing, admin replay endpoint for failed outbox events, saga timeout/outbox-failure metrics, and scheduled outbox/dedup cleanup jobs. | Alert tuning and runbook-driven on-call drills. |
| Payment Service | 90% | In Progress | Intent/webhook/idempotency + consumer dedup + outbox-based payment result publishing implemented, scheduled outbox/dedup cleanup jobs, webhook HMAC signature verification, simulated provider outage mode, retry-to-DLQ handling, dead-letter requeue APIs, and outage observability counters. | Add runbook-driven outage drill execution with alert thresholds tied to provider retry/DLQ spikes. |
| Review Service | 10% | Not Started | Scaffold + health only. | Implement rating/review CRUD + moderation model. |
| Search Service | 93% | In Progress | Ranking/reindex hardening plus consumer dedup, expanded relevance dataset, target pass-rate evaluation (`meetsTarget`), dataset health endpoint, and passing contract/unit tests for relevance workflows. | Add CI freshness gate + periodic dataset refresh ownership automation. |
| Notification Service | 91% | In Progress | SMTP/log provider options, template engine, DLQ persistence/publish/requeue, alert topic publish, consumer dedup, metrics counters, and scheduled dedup cleanup implemented; Prometheus/Grafana thresholds tuned. | Production provider credentials + runbook validation under failure drills. |

## Platform and Cross-Cutting

| Area | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| Docker Compose Infra | 70% | In Progress | MySQL/MongoDB/Redis/Kafka/ES/Zipkin/Prometheus/Grafana are provisioned. | Add stronger healthchecks/init scripts and persistent tuning. |
| OpenAPI Coverage | 82% | In Progress | Springdoc wired in active services and `API_DOCS.md` now includes endpoint, entity, store, and flow details per service/gateway. | Align contracts and standardize response/error schemas with generated specs. |
| Lombok Adoption | 70% | In Progress | Core entities/models migrated in active services. | Complete remaining classes and standardize style. |
| SOLID (DIP) Structure | 60% | In Progress | Controllers/consumers now depend on service interfaces in active modules. | SRP split large services and formalize orchestration patterns. |
| Kafka Contracts | 60% | In Progress | Order/payment/search plus inventory reservation outcome events and saga consumers are wired. | Schema versioning, DLQ strategy, contract tests. |
| Outbox + Global Idempotency | 96% | In Progress | Shared reliability library extracted in common-core and adopted by order/payment/inventory/search/notification dedup/outbox flows, with scheduled cleanup automation added. | Optional adapter consolidation and retention tuning per environment. |
| Observability in App Code | 76% | In Progress | Added Prometheus registry and Zipkin tracing baseline across gateway/auth/product/cart/inventory/order/payment/search/notification with trace-log correlation pattern, Alertmanager routing now includes payment retry/DLQ alerts, and outage drill runbooks are in place. | Replace placeholder receiver endpoints with production secrets/routes and add KPI dashboards per service. |
| CI/CD | 72% | In Progress | Added GitHub Actions backend quality workflow and staged release pipeline (`quality -> package -> staging -> production`) with environment-scoped promotion gates, post-deploy smoke checks, and rollback trigger hooks. | Wire real staging/prod secrets/webhooks and add rollback success verification callbacks. |
| Load Testing | 45% | In Progress | Added k6 flash-sale inventory scenario plus baseline browse/cart/checkout suites with explicit p95/failure/success thresholds and runnable PowerShell orchestration scripts. | Add CI-triggered performance regression gates and environment-specific budget tuning. |

## Immediate Execution Order
1. Search dataset freshness ownership rota + review cadence.
2. Alertmanager production receiver routes (Slack/PagerDuty/email) + ownership mapping.
3. Promote rollback verification callbacks into release gate dashboard.
4. CI-triggered load regression stage with budget-based pass/fail.
5. Search dataset ownership rota automation execution (cadence + reviewer assignment).

