# API Docs - API Gateway

Public entry base: `/api/*`

## Endpoints
- Gateway routes incoming `/api/*` traffic to downstream microservices.
- Fallback endpoint: `GET /fallback/{service}` for circuit-breaker failover response.
- Internal release gate callback endpoint: `POST /internal/release-gate/callbacks` (records rollback callback telemetry for Prometheus/Grafana).
- Internal frontend flags endpoint: `GET /internal/frontend-flags` (beta/admin UI runtime toggles).

## Entities
- No domain entities persisted in gateway.
- Runtime payload: standardized error response envelope.

## Data Stores
- Redis: route-level rate limiting.
- Kafka: not engaged at gateway.
- Auth service dependency: token validation via `/api/auth/validate`.
- Prometheus (via Micrometer): rollback callback counters
  - `release_gate_rollback_callback_total{event,environment,status}`
  - `release_gate_rollback_callback_failure_total{event,environment}`

## Flow
1. Request enters gateway route.
2. Correlation ID is attached/propagated.
3. API version guard validates `X-API-Version` for protected `/api/**` paths.
4. JWT guard applies policy via `GatewayAuthRoutePolicy` (public read routes bypass token; protected writes require token).
5. `AuthValidationClient` calls auth-service `/api/auth/validate` for bearer-token activity checks.
6. Redis rate limit filter is applied per route.
7. Circuit breaker routes failures to `/fallback/{service}`.
8. Request is forwarded to target microservice.

### Internal Callback Metrics Flow
1. Release pipeline posts rollback verification payload to external callback receiver (`CALLBACK_WEBHOOK_URL`).
2. The same payload is optionally posted to gateway internal endpoint (`CALLBACK_METRICS_URL` -> `/internal/release-gate/callbacks`).
3. Gateway increments release-gate callback counters for dashboard/alert queries.
4. Prometheus scrapes gateway `/actuator/prometheus`, and Grafana panels read these counters.

### Internal Frontend Flags Flow
1. Frontend requests `/internal/frontend-flags` on startup.
2. Gateway returns runtime toggles (`betaBannerEnabled`, `adminConsoleEnabled`).
3. Frontend merges gateway flags with local env flags for safe rollout control.
