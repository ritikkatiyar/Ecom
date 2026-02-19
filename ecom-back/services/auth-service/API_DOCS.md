# API Docs - Auth Service

Base path: `/api/auth`

## Endpoints
- `POST /signup` - register account.
- `POST /login` - issue access + refresh tokens.
- `POST /refresh` - issue new access token from refresh token.
- `POST /logout` - blacklist access token.
- `GET /validate` - validate bearer token (used by gateway).

## Entities
- `UserAccount` (MySQL)
- `RefreshToken` (MySQL)

## Data Stores
- MySQL: users + refresh tokens.
- Redis: access-token blacklist.
- Kafka: configured, no producer/consumer flow yet in auth APIs.

## Flow
1. Client hits auth endpoint.
2. Service validates payload and user credentials.
3. Writes/reads `UserAccount` and `RefreshToken` in MySQL.
4. On logout, token id is added to Redis blacklist.
5. `GET /validate` checks JWT integrity + blacklist status.
