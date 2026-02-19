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
1. `CartController` delegates to `CartUseCases` (`CartService`) for orchestration.
2. `CartOwnerResolver` validates ownership (`userId` xor `guestId`) and resolves request scope.
3. `UserCartStore` handles authenticated cart operations with `CartItemRepository` on MySQL.
4. `GuestCartStore` handles guest cart operations in Redis with 7-day TTL refresh on writes.
5. Merge flow pulls guest Redis entries, upserts MySQL rows, then clears guest key.
