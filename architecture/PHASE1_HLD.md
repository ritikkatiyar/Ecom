# Amazon Lite - Production Architecture (Phase 1 HLD)

## 1. Goals and Non-Goals

### Goals
- Build an Amazon-style e-commerce platform with microservices, async workflows, and independent scaling.
- Prioritize resilience, observability, and performance over monolithic simplicity.
- Support eventual consistency where needed (order/payment/inventory flows).

### Non-Goals (for MVP)
- Full marketplace seller onboarding.
- Global multi-region active-active writes.
- Real-time shipment carrier integrations beyond webhook simulation.

## 2. System Context

- Frontend: Next.js (SSR + client interactivity)
- Entry: API Gateway (auth, routing, rate limits)
- Backend: Spring Boot microservices
- Messaging: Kafka for asynchronous event-driven communication
- Data stores: MySQL, MongoDB, Redis, Elasticsearch (polyglot persistence)

## 3. Core Services and Responsibilities

1. Auth Service
- Signup/login
- JWT access + refresh token lifecycle
- OAuth2 social login (Google)
- Token revocation/blacklist via Redis

2. User Service
- User profile, address book, preferences
- User-level policy and metadata

3. Product Service
- Product CRUD
- Categories, brands, variants
- Media and attributes for indexing

4. Inventory Service
- Stock by SKU/warehouse
- Reservation and release
- Atomic stock deduction (prevent oversell)

5. Cart Service
- Guest cart in Redis
- Authenticated cart persistence
- Cart merge on login

6. Order Service
- Order lifecycle and state transitions
- Saga orchestration/choreography hooks

7. Payment Service
- Payment intent/session creation
- Gateway integration (Razorpay)
- Webhooks + idempotent state transitions

8. Review Service
- Ratings, reviews, moderation flags
- Product rating aggregates

9. Search Service
- Indexing pipeline
- Query APIs: fuzzy, autocomplete, ranking

10. Notification Service
- Email/event notifications
- Consumer for order/payment lifecycle events

## 4. High-Level Architecture

Client (Next.js)
-> API Gateway
-> Synchronous service APIs (REST)
-> Kafka async bus for domain events
-> Datastores per service

Design constraints:
- Database-per-service ownership.
- No cross-service direct DB reads.
- Communication split:
  - Sync for user-facing low-latency paths.
  - Async for eventual consistency and side effects.

## 5. Data Strategy (Initial)

- Auth Service: MySQL
- User Service: MySQL
- Product Service: MongoDB
- Inventory Service: MySQL
- Cart Service: Redis (+ optional MySQL snapshot)
- Order Service: MySQL
- Payment Service: MySQL
- Review Service: MongoDB or MySQL (start with MySQL for consistency)
- Search Service: Elasticsearch
- Notification Service: Stateless + provider templates

## 6. API Gateway Responsibilities

- JWT verification and claims propagation
- Rate limiting and burst control
- Request routing and API versioning
- Request correlation ID injection
- Optional response caching for read endpoints

## 7. Event-Driven Contracts (Kafka)

Topics:
- order.created.v1
- inventory.reserved.v1
- inventory.rejected.v1
- payment.authorized.v1
- payment.failed.v1
- order.confirmed.v1
- order.cancelled.v1
- notification.email.requested.v1
- product.updated.v1 (for search reindex)

Envelope standard:
- eventId (UUID)
- eventType
- occurredAt (ISO8601)
- producer
- schemaVersion
- traceId
- payload (domain data)

Reliability patterns:
- Outbox pattern in producer services
- Idempotent consumers using eventId de-dup table/cache
- Dead-letter topics per critical stream

## 8. Order Saga (Baseline Flow)

1. Order Service creates order (CREATED / PAYMENT_PENDING) and emits order.created.v1.
2. Inventory Service attempts reservation:
- success -> inventory.reserved.v1
- failure -> inventory.rejected.v1
3. Payment Service on reservation success attempts authorization:
- success -> payment.authorized.v1
- failure -> payment.failed.v1
4. Order Service updates state:
- payment success -> CONFIRMED
- payment failure or no stock -> CANCELLED
5. Notification Service sends corresponding email/SMS.

Compensations:
- If payment fails after reservation: emit inventory.release.requested.v1.
- If downstream timeout: retry with bounded backoff + alert.

## 9. Consistency Model

- Strong consistency inside a single service transaction boundary.
- Eventual consistency across services.
- Client-visible states include transient values (PAYMENT_PENDING, RESERVATION_PENDING).

## 10. Caching Strategy

- Redis:
  - token blacklist
  - cart storage
  - product query cache (short TTL)
  - idempotency key cache
- Invalidation:
  - event-driven invalidation on product/inventory updates

## 11. Security Baseline

- OAuth2 + JWT, rotating refresh tokens
- Service-to-service auth (mTLS or signed internal tokens)
- Secret management via vault/env-injector
- PII encryption at rest and TLS in transit
- Webhook signature verification for payment provider

## 12. Observability Baseline

- Logs: structured JSON logs with traceId/spanId
- Metrics: Prometheus + Grafana
- Tracing: OpenTelemetry + Zipkin/Jaeger
- SLO examples:
  - P95 product search latency < 300 ms
  - Order success ratio > 99%
  - Payment callback processing < 2 min

## 13. Deployment Topology

Local:
- Docker Compose for services + Kafka + Redis + DBs + Elasticsearch

Production:
- Kubernetes (EKS/AKS/GKE)
- NGINX/Ingress controller
- HPA on CPU/RPS/queue lag
- Blue-green or canary rollout

CI/CD:
- GitHub Actions
- per-service test/build/scan/deploy pipeline
- schema compatibility checks for events

## 14. Frontend Integration Model (Next.js)

- SSR for SEO-critical product/category pages
- React Query for client data synchronization
- Debounced search against Search Service
- Optimistic cart updates with rollback on rejection
- Unified API client layer with retries and error normalization

## 15. 90-Day Execution Plan

Month 1 (Core)
- Week 1: Auth + User + Product
- Week 2: Inventory + Cart
- Week 3: Order + Payment
- Week 4: Kafka integration + outbox + basic saga

Month 2 (Scale Foundations)
- Redis caching and idempotency
- Elasticsearch indexing/query relevance
- Dockerization and compose stack
- Observability stack integration

Month 3 (Experience + Production)
- Next.js production frontend integration
- End-to-end hardening and resilience patterns
- Kubernetes deployment + CI/CD
- k6 load test for 10k virtual users scenario

## 16. Initial Risks and Mitigations

- Distributed transaction complexity:
  - Mitigation: strict saga contracts + idempotency and retries.
- Event schema drift:
  - Mitigation: versioned schemas and contract tests.
- Overselling in flash sale:
  - Mitigation: reservation-first workflow + atomic stock operations + locks.
- Debuggability issues:
  - Mitigation: mandatory trace IDs and end-to-end distributed tracing.

## 17. Definition of Done for Phase 1

- Service boundaries documented and reviewed.
- API + event contracts versioned.
- Data ownership finalized.
- Local compose architecture runs end-to-end for happy-path order flow.
- Basic dashboards/traces visible for key transactions.

