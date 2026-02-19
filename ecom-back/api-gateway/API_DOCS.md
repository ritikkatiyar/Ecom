# API Docs - API Gateway

Public entry base: `/api/*`

## Endpoints
- Gateway routes incoming `/api/*` traffic to downstream microservices.
- Fallback endpoint: `GET /fallback/{service}` for circuit-breaker failover response.

## Entities
- No domain entities persisted in gateway.
- Runtime payload: standardized error response envelope.

## Data Stores
- Redis: route-level rate limiting.
- Kafka: not engaged at gateway.
- Auth service dependency: token validation via `/api/auth/validate`.

## Flow
1. Request enters gateway route.
2. Correlation ID is attached/propagated.
3. API version guard validates `X-API-Version` for protected `/api/**` paths.
4. JWT guard applies policy (public read routes bypass token; protected writes require token).
5. Redis rate limit filter is applied per route.
6. Circuit breaker routes failures to `/fallback/{service}`.
7. Request is forwarded to target microservice.
