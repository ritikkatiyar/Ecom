# Backend Remaining Points

Last updated: 2026-03-30

## Phase-level Backlog

| Phase | Item | Status |
|-------|------|--------|
| **Phase 2** | Final production hardening loop (threshold tuning, receiver validation, remaining contract depth) | Pending |
| **Phase 3** | Final staged/prod rollback drill execution evidence + tuning deltas | Pending (blocked without staging/prod) |
| **Phase 5** | Production hosting topology finalization (Vercel frontend + Cloudflare edge/JVM-compatible backend + MongoDB Atlas + Supabase Postgres) | In Progress |
| **Phase 5** | Blue-green / runtime release controls | Pending |
| **Phase 6** | Production receiver integrations fully validated end-to-end | Pending |
| **Phase 7** | Blue-green strategy | Pending |
| **Phase 7** | Full API version lifecycle policy | Pending |
| **Phase 8** | 10k-user sustained scenario validation + production-profile budget signoff | Pending |

---

## Per-Service Gaps

| Service | % | Next Critical Step |
|---------|---|--------------------|
| **API Gateway** | 97% | Breaker-threshold tuning by traffic profile; production receiver/alert mapping drills |
| **Auth Service** | 85% | Token rotation hardening, audits, integration tests, gateway policy hookup |
| **User Service** | 58% | Integration tests + auth-policy hookup; consider event publishing for profile updates |
| **Product Service** | 75% | Variants depth, indexing events, stricter validation/versioning |
| **Inventory Service** | 96% | Flash-sale load profile assertions (p95 latency, oversell SLO) in k6 |
| **Cart Service** | 78% | Price snapshot/validation + cart eventing |
| **Order Service** | 94% | Alert tuning; runbook-driven on-call drills |
| **Payment Service** | 93% | Runbook-driven outage drill execution; alert thresholds tied to retry/DLQ spikes |
| **Review Service** | 56% | Integration tests + gateway auth policy for create/update/delete and moderation |
| **Search Service** | 99% | Reviewer pool sync with active on-call owners; team membership changes |
| **Notification Service** | 91% | Production provider credentials; runbook validation under failure drills |

---

## Platform & Cross-Cutting

| Area | Next Step |
|------|-----------|
| **Deployment Decision** | Frontend deploys to Vercel; product Mongo moves to MongoDB Atlas free tier; relational services target Supabase Postgres; Cloudflare can front the backend, but current Spring Boot services still need a JVM-capable runtime unless rewritten for Workers |
| **Relational DB Migration** | Replace current MySQL assumptions in config, Liquibase, SQL compatibility, and integration tests with Postgres/Supabase-compatible equivalents |
| **Docker Compose** | Stronger healthchecks, init scripts, persistent tuning |
| **OpenAPI** | Align contracts; standardize response/error schemas |
| **Lombok** | Complete remaining classes; standardize style |
| **SOLID/DIP** | Continue SRP cleanup; formalize orchestration patterns |
| **Kafka Contracts** | Schema depth; backward-compatibility checks; producer/consumer contract tests for product + notification |
| **Outbox** | Optional adapter consolidation; retention tuning per environment |
| **Observability** | Validate calibrated thresholds vs staged/prod drill results; tune noisy alerts |
| **CI/CD** | Wire real staging/prod callback endpoints; complete non-missing drill evidence cycle |
| **Load Testing** | Periodic threshold review signoff; keep calibration evidence tied to budget changes |

---

## Schema Migration (Liquibase)

**Pending Liquibase rollout:**
- cart-service
- user-service
- review-service
- notification-service

**Already on Liquibase:** order-service, inventory-service, payment-service, auth-service

---

## Major Backend Gaps (from PROJECT_PROGRESS_SUMMARY)

1. **User & Review hardening** – Integration tests, auth-policy coupling, eventing decisions
2. **Event architecture** – Schema/versioning discipline; outbox rollout; retries; DLQ; consumer idempotency
3. **Saga observability** – Compensation visibility; runbookize replay/cleanup
4. **API Gateway** – Breaker-threshold tuning; policy regression coverage
5. **Resilience** – Consistent retry/circuit-breaker/timeout patterns
6. **Observability** – Business KPI dashboards; production receiver integrations
7. **Search quality** – Refresh ownership automation; cadence governance

---

## Suggested Execution Order

1. Finalize edge deployment shape in `EDGE_DEPLOYMENT_PLAN.md` and keep Cloudflare as edge-only unless a Worker rewrite is approved
2. Migrate relational-service configuration and schema assumptions from MySQL to Supabase Postgres
3. Replace Alertmanager placeholder webhooks with production receiver routes
4. Add deploy pipeline stages with environment promotion checks
5. Complete user/review services (integration tests + auth-policy)
6. Add Liquibase to auth, cart, user, review, notification services with Postgres compatibility validation
7. Breaker-threshold tuning from staged/prod traffic
8. Runbook-driven outage and rollback drills with threshold signoff
