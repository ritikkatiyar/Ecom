# API Docs - API Gateway

Public entry base: `/api/*`

## Endpoints
- Gateway routes incoming `/api/*` traffic to downstream microservices.
- Fallback endpoint: `/fallback/{service}` (GET, POST, etc.) for circuit-breaker failover response.
- Internal release gate callback endpoint: `POST /internal/release-gate/callbacks` (records rollback callback telemetry for Prometheus/Grafana).
- Internal frontend flags endpoint: `GET /internal/frontend-flags` (beta/admin UI runtime toggles).

## Entities
- No domain entities persisted in gateway.
- Runtime payload: standardized error response envelope.

## Data Stores
- Redis: rate limiting (per-IP, 120 req/60s default). Falls back to in-memory when Redis unavailable.
- Request logging: every API request logged (method, path, status, duration, correlation-id, client IP).
- Kafka: not engaged at gateway.
- Auth service dependency: token validation via `/api/auth/validate` (timeout configurable, default 15s; logs 401/503 causes).
- Prometheus (via Micrometer): rollback callback counters
  - `release_gate_rollback_callback_total{event,environment,status}`
  - `release_gate_rollback_callback_failure_total{event,environment}`

## Request Filters

Filters run in **ascending order** (lowest order value first). All are `GlobalFilter`s unless noted.

| Order | Filter | Purpose |
|------:|--------|---------|
| -500 | `RequestLoggingFilter` | Logs every API request after response |
| -300 | `CorrelationIdFilter` | Attaches/propagates `X-Correlation-Id` |
| -250 | `ApiVersionFilter` | Validates `X-API-Version` for non-auth `/api/**` |
| -200 | `RateLimitWithFallbackFilter` | Per-IP rate limiting |
| -150 | `JwtAuthFilter` | JWT validation for protected routes |

### Filter Details

#### 1. RequestLoggingFilter (order -500)
- **Purpose**: Audit and observability. Logs every API request.
- **When**: Runs first; logs in `doFinally` after the chain completes (so status and duration are known).
- **Log fields**: `method`, `path`, `status`, `durationMs`, `correlationId`, `clientIp`.
- **Client IP**: Uses `X-Forwarded-For` (first hop) or remote address.
- **Skips**: None; logs all API traffic.

#### 2. CorrelationIdFilter (order -300)
- **Purpose**: Distributed tracing. Ensures every request has a correlation ID.
- **When**: Early in chain so downstream filters and services can use it.
- **Behavior**:
  - If `X-Correlation-Id` present, keeps it.
  - Otherwise generates a new UUID.
  - Adds/overwrites header on request and sets it on response.
- **Downstream**: Propagated to backend services for log correlation.

#### 3. ApiVersionFilter (order -250)
- **Purpose**: API versioning. Enforces `X-API-Version` on non-auth paths.
- **Skips**: Paths not under `/api/`, or under `/api/auth/` (login/signup/validate).
- **Behavior**:
  - For `/api/**` (except `/api/auth/**`), checks `X-API-Version` header.
  - Required value from `app.gateway.api-version.required` (default `v1`).
  - If missing or wrong, returns `400 BAD_REQUEST` with `API_VERSION_MISMATCH`.
- **Error**: Uses `GatewayErrorWriter` for standardized JSON error.

#### 4. RateLimitWithFallbackFilter (order -200)
- **Purpose**: Abuse protection. Limits requests per client IP.
- **Skips**: `/internal/**`, `/actuator/**`.
- **Behavior**:
  - **mode=redis**: Tries Redis; on error falls back to in-memory.
  - **mode=in-memory**: Uses in-memory only.
  - Per-IP sliding window (default 120 req/60s).
  - Client key: `X-Forwarded-For` or remote address.
- **Limit exceeded**: Returns `429 TOO_MANY_REQUESTS` with `RATE_LIMIT_EXCEEDED`.
- **Fallback**: In-memory limits per gateway instance when Redis fails.

#### 5. JwtAuthFilter (order -150)
- **Purpose**: Authorization. Validates JWT for protected routes.
- **Skips**:
  - `OPTIONS` (CORS preflight).
  - Routes not considered protected by `GatewayAuthRoutePolicy`.
- **Protected routes** (require Bearer token):
  - `/api/cart`, `/api/orders`, `/api/payments`, `/api/inventory`, `/api/users`, `/api/reviews` – all methods.
  - `/api/products`, `/api/search` – only non-GET (POST, PUT, PATCH, DELETE).
- **Public**: `/api/auth/**`, `GET` on `/api/products/**`, `GET` on `/api/search/**`, `/internal/**`, `/actuator/**`.
- **Behavior**:
  - If protected and no `Authorization: Bearer <token>`, returns `401 AUTH_TOKEN_MISSING`.
  - If protected, calls auth-service `GET /api/auth/validate` with Bearer token.
  - If token invalid/expired/blacklisted, returns `401 AUTH_TOKEN_INVALID`.
  - If auth-service unreachable, returns `503 AUTH_VALIDATION_UNAVAILABLE`.
- **Collaborators**: `GatewayAuthRoutePolicy` (route policy), `AuthValidationClient` (validate call).

#### 6. Circuit Breaker (route-level, Spring Cloud Gateway)
- **Purpose**: Fault tolerance. Stops calling a failing backend and routes to fallback.
- **When**: Per route; applied to each downstream route (auth, product, cart, etc.).
- **Behavior**:
  - On timeout or failure, forwards to `forward:/fallback/{service}`.
  - Original method (e.g. POST) is preserved; `FallbackController` supports all methods.
  - Returns `503 SERVICE_UNAVAILABLE` with `DOWNSTREAM_UNAVAILABLE`.
- **Timeouts**: Per route via `metadata.response-timeout`; auth-circuit 15s, others 3–5s.

---

## Flow (Summary)

1. Request enters gateway.
2. **RequestLoggingFilter** starts (logs on completion).
3. **CorrelationIdFilter** adds/propagates `X-Correlation-Id`.
4. **ApiVersionFilter** validates `X-API-Version` (skips `/api/auth/**`).
5. **JwtAuthFilter** validates JWT for protected routes (skips public paths).
6. **RateLimitWithFallbackFilter** enforces per-IP limits (skips /internal/, /actuator/).
7. **Circuit breaker** (route-level) forwards to backend or fallback on failure.
8. Request is proxied to target microservice.

### Internal Callback Metrics Flow
1. Release pipeline posts rollback verification payload to external callback receiver (`CALLBACK_WEBHOOK_URL`).
2. The same payload is optionally posted to gateway internal endpoint (`CALLBACK_METRICS_URL` -> `/internal/release-gate/callbacks`).
3. Gateway increments release-gate callback counters for dashboard/alert queries.
4. Prometheus scrapes gateway `/actuator/prometheus`, and Grafana panels read these counters.

### Internal Frontend Flags Flow
1. Frontend requests `/internal/frontend-flags` on startup.
2. Gateway returns runtime toggles (`betaBannerEnabled`, `adminConsoleEnabled`).
3. Frontend merges gateway flags with local env flags for safe rollout control.
