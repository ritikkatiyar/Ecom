# Amazon Lite Phase Completion Checklist

Last updated: 2026-03-08

## Phase 1 - System Design (HLD)
Status: `Done`
- [x] Microservice boundaries defined (auth, user, product, inventory, cart, order, payment, review, search, notification).
- [x] Core data strategy documented (MySQL, MongoDB, Redis, Elasticsearch).
- [x] API gateway + async/event-driven architecture documented.
- [x] Phase-1 HLD maintained in `architecture/PHASE1_HLD.md`.

## Phase 2 - Backend Build (Spring Boot)
Status: `In Progress (~99% core, remaining hardening)`
- [x] Gateway, auth, product, inventory, cart, order, payment, search, notification core flows implemented.
- [x] Outbox + consumer dedup + cleanup schedulers on critical services.
- [x] Release pipeline with smoke/rollback gates and rollback callback visibility.
- [x] Load suites and regression jobs (staging + production read-heavy profile).
- [x] User service domain APIs (profile/address/preferences) implemented.
- [x] Review service domain APIs (CRUD/moderation) implemented.
- [ ] Final production hardening loop (threshold tuning, receiver validation, remaining contract depth) pending.

## Phase 3 - Event-Driven Architecture
Status: `In Progress (advanced)`
- [x] Kafka event choreography for order/payment/inventory/search/notification.
- [x] Idempotent consumers + outbox replay/recovery mechanisms.
- [x] Rollback callback telemetry + calibrated alert rules + ops drill cadence.
- [x] Schema governance tightening across event contracts (registry + schema files + CI validator gate).
- [ ] Final staged/prod rollback drill execution evidence + tuning deltas pending.

## Phase 4 - Frontend (Next.js Production Style)
Status: `In Progress (70%)`
- [x] Frontend kickoff started with beta release scaffolding (feature flags + admin guard baseline).
- [x] Next.js storefront scaffolded (`ecom-storefront`) with App Router and stitch design system.
- [x] Core routes: home, shop, products/[id], search, cart, account, collections.
- [x] Header nav component, API proxy via `NEXT_PUBLIC_BACKEND_URL`.
- [x] Wire backend APIs (products, search, cart, auth) to storefront. Products admin CRUD + image upload + delete done; shop Add-to-Cart; image upload retry + auto-dismiss.
- [x] SSR product pages + debounced search + optimistic cart.
- [x] Infinite scroll on shop page (useInfiniteQuery + IntersectionObserver).
- [x] State/query architecture (React Query for server state).

## Phase 5 - Deployment Architecture
Status: `In Progress`
- [x] Docker-based local infra and service orchestration baseline.
- [x] CI workflows for quality/release/load calibration/drill cadence.
- [ ] Production hosting topology finalization (ECS/EC2/etc.) pending.
- [ ] Blue-green/runtime release controls pending.

## Phase 6 - Observability
Status: `In Progress`
- [x] Prometheus + Grafana + Alertmanager + Zipkin baseline integrated.
- [x] Service-level metrics and release-gate dashboards/alerts added.
- [ ] Production receiver integrations fully validated end-to-end pending.

## Phase 7 - Advanced Production Features
Status: `Partially In Progress`
- [x] Rate limiting and circuit breakers baseline.
- [x] Idempotency + retry/DLQ on key flows.
- [x] Feature flags (gateway `app.frontend-flags.*`, storefront FlagsContext, BetaBanner, admin-console gating).
- [ ] Blue-green strategy pending.
- [ ] Full API version lifecycle policy pending.

## Phase 8 - Scale Simulation
Status: `In Progress`
- [x] k6 flash-sale + browse/cart/checkout suites.
- [x] CI-based regression gates and budget calibration workflow.
- [ ] 10k-user sustained scenario validation + production-profile budget signoff pending.

## Immediate Next 3 Execution Items
1. Execute and review scheduled release-gate drill artifacts until `check_release_gate_drill_evidence.py` passes (staging + production non-missing), then record deltas in `ecom-back/infrastructure/runbooks/DEPLOY_SMOKE_ROLLBACK.md`.
2. Schema governance baseline is now enforced via `ecom-back/contracts/events/event-contracts.json` and `check_event_contracts.py`.
3. ~~Wire ecom-storefront to backend APIs (products/search/cart/auth) and complete browse/search/cart flows.~~ Done: browse/search/cart/auth wired; shop Add-to-Cart; product delete; image upload UX (retry, auto-dismiss).

