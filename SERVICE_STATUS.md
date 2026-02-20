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
| User Service | 58% | In Progress | Added profile/address/preferences APIs on MySQL with DIP-aligned `UserUseCases` service boundary and default-address normalization flow. | Add integration tests + auth-policy hookup and consider event publishing for profile updates. |
| Product Service | 70% | In Progress | CRUD + pagination/filter/sort implemented on MongoDB. | Variants depth, indexing events, stricter validation/versioning. |
| Inventory Service | 96% | In Progress | Saga consumers now include consumer dedup, outbox-based publishing, compensation handling for `order.timed-out` events, scheduled outbox/dedup cleanup jobs, reservation expiry scheduler, concurrency contract tests, and real MySQL+Kafka integration tests for contention/dedup scenarios. | Add flash-sale load profile assertions (p95 latency, oversell SLO) in k6. |
| Cart Service | 78% | In Progress | Guest Redis + user MySQL cart + merge implemented, plus SRP split (`CartOwnerResolver`, `GuestCartStore`, `UserCartStore`) with controller DIP on `CartUseCases`. | Price snapshot/validation + cart eventing. |
| Order Service | 94% | In Progress | Added timeout sweep for stale payment-pending orders, timeout event publishing, admin replay endpoint for failed outbox events, saga timeout/outbox-failure metrics, scheduled outbox/dedup cleanup jobs, and SRP split (`OrderItemCodec`, `OrderResponseMapper`, `OrderEventPublisher`) for cleaner orchestration boundaries. | Alert tuning and runbook-driven on-call drills. |
| Payment Service | 90% | In Progress | Intent/webhook/idempotency + consumer dedup + outbox-based payment result publishing implemented, scheduled outbox/dedup cleanup jobs, webhook HMAC signature verification, simulated provider outage mode, retry-to-DLQ handling, dead-letter requeue APIs, and outage observability counters. | Add runbook-driven outage drill execution with alert thresholds tied to provider retry/DLQ spikes. |
| Review Service | 56% | In Progress | Implemented rating/review CRUD + moderation status model (`PENDING/APPROVED/REJECTED`) on MySQL with DIP-aligned `ReviewUseCases` service boundary. | Add integration tests + gateway auth policy for create/update/delete and moderation paths. |
| Search Service | 99% | In Progress | Ranking/reindex hardening plus consumer dedup, expanded relevance dataset, target pass-rate evaluation (`meetsTarget`), dataset health endpoint, passing relevance tests, ownership rotation automation, enforced minimum reviewer pool policy, and collaborator-permission validation in rota workflow. | Keep reviewer pool synchronized with active on-call owners and team membership changes. |
| Notification Service | 91% | In Progress | SMTP/log provider options, template engine, DLQ persistence/publish/requeue, alert topic publish, consumer dedup, metrics counters, and scheduled dedup cleanup implemented; Prometheus/Grafana thresholds tuned. | Production provider credentials + runbook validation under failure drills. |

## Platform and Cross-Cutting

| Area | % Complete | Status | Current State | Next Critical Step |
|---|---:|---|---|---|
| Docker Compose Infra | 70% | In Progress | MySQL/MongoDB/Redis/Kafka/ES/Zipkin/Prometheus/Grafana are provisioned. | Add stronger healthchecks/init scripts and persistent tuning. |
| OpenAPI Coverage | 82% | In Progress | Springdoc wired in active services and `API_DOCS.md` now includes endpoint, entity, store, and flow details per service/gateway. | Align contracts and standardize response/error schemas with generated specs. |
| Lombok Adoption | 70% | In Progress | Core entities/models migrated in active services. | Complete remaining classes and standardize style. |
| SOLID (DIP) Structure | 78% | In Progress | Controllers/consumers now depend on service interfaces in active modules, including user-service (`UserController` -> `UserUseCases`), review-service (`ReviewController` -> `ReviewUseCases`), cart-service (`CartController` -> `CartUseCases`), and order-service SRP decomposition (`OrderItemCodec`, `OrderResponseMapper`, `OrderEventPublisher`). | Continue SRP split of remaining high-complexity services and formalize orchestration patterns. |
| Kafka Contracts | 60% | In Progress | Order/payment/search plus inventory reservation outcome events and saga consumers are wired. | Schema versioning, DLQ strategy, contract tests. |
| Outbox + Global Idempotency | 96% | In Progress | Shared reliability library extracted in common-core and adopted by order/payment/inventory/search/notification dedup/outbox flows, with scheduled cleanup automation added. | Optional adapter consolidation and retention tuning per environment. |
| Observability in App Code | 95% | In Progress | Added Prometheus registry and Zipkin tracing baseline across gateway/auth/product/cart/inventory/order/payment/search/notification with trace-log correlation pattern, Alertmanager routing includes payment retry/DLQ alerts, Grafana has release-gate rollback callback telemetry dashboard panels, gateway exposes internal rollback callback metric emitter endpoint, calibrated release-gate rules are in place, weekly ops receiver drill cadence is wired, receiver config validation blocks placeholder/malformed production settings, and scheduled staged/prod release-gate drill workflow now auto-generates delta reports. | Validate calibrated thresholds against staged/prod drill results and tune noisy alerts. |
| CI/CD | 92% | In Progress | Added GitHub Actions backend quality workflow and staged release pipeline (`quality -> package -> staging -> production`) with promotion gates, smoke/rollback checks, rollback verification callbacks, release-gate summary artifacts, staging load regression, and production read-heavy load regression profile. | Wire real staging/prod callback endpoints and calibrate budgets from production telemetry. |
| Load Testing | 90% | In Progress | Added k6 flash-sale inventory scenario plus baseline browse/cart/checkout suites with explicit thresholds, local runners, CI-triggered staging regression gate, production-aware read-heavy regression profile, weekly calibration workflow, Prometheus auto-ingestion for observed budgets, and applied approved production p95 threshold deltas in release workflow. | Enforce periodic threshold review signoff and keep calibration evidence attached to budget changes. |

## Immediate Execution Order
1. Search dataset freshness ownership rota + review cadence.
2. Alertmanager production receiver routes (Slack/PagerDuty/email) + ownership mapping.
3. Execute staged/prod rollback drills and capture threshold tuning deltas in runbook from generated artifacts (delta-log automation wired; awaiting non-missing staged/prod drill results).
4. Frontend phase kickoff once backend core pending services are feature-complete.
5. Raise SOLID maturity from DIP baseline by SRP split of remaining high-complexity services (payment next).


