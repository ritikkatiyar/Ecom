# API Docs - Cart Service

Base path: `/api/cart`

## Endpoints
- `POST /items` - add/update item.
- `GET /` - fetch cart.
- `DELETE /items/{productId}` - remove item.
- `DELETE /` - clear cart.
- `POST /merge` - merge guest cart into user cart.

## Entities
- `CartItem` / user cart entity (MySQL)
- Guest cart key/value structures (Redis)

## Data Stores
- MySQL: logged-in user cart persistence.
- Redis: guest cart and fast cart operations.
- Kafka: not yet engaged for cart events.

## Flow
1. Guest operations read/write Redis cart keys.
2. Authenticated operations persist/read MySQL cart rows.
3. Merge API reads guest cart from Redis, upserts user cart rows in MySQL, then clears guest cart.
