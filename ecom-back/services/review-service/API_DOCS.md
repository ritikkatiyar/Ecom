# API Docs - Review Service

Base path: `/api/reviews`

## Endpoints
- `POST /` - create review (`userId` query param required).
- `PUT /{reviewId}` - update own review (`userId` query param required).
- `DELETE /{reviewId}` - delete own review (`userId` query param required).
- `GET /{reviewId}` - fetch review by id.
- `GET /?productId=...&includePending=false` - list product reviews.
- `GET /by-user?userId=...` - list user reviews.
- `POST /{reviewId}/moderate` - moderation status update (`APPROVED|REJECTED|PENDING`).

## Entities
- `ReviewRecord` (MySQL)
- `ReviewStatus` enum: `PENDING`, `APPROVED`, `REJECTED`

## Data Stores
- MySQL: review/rating/moderation persistence.
- Redis: not engaged.
- Kafka: not engaged.

## Flow
1. Controller depends on `ReviewUseCases` interface (DIP boundary).
2. Create/update requests set review status to `PENDING` for moderation.
3. Public product listing returns `APPROVED` reviews unless `includePending=true`.
4. Moderation endpoint updates review status and supports review workflow state transitions.
