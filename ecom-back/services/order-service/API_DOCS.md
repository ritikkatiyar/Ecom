# API Docs - Order Service

Base path: `/api/orders`

## Endpoints
- `POST /` - create order.
- `GET /{orderId}` - get order details.
- `GET /?userId=...` - list user orders.
- `POST /{orderId}/cancel` - cancel order.
- `POST /{orderId}/confirm` - confirm order.
- `POST /admin/saga/timeouts/run` - manually trigger timeout sweep.
- `POST /admin/outbox/replay-failed` - replay failed outbox records.

## Entities
- `OrderRecord` (MySQL)
- `OutboxEventRecord` (MySQL)
- `ConsumedEventRecord` (MySQL)

## Data Stores
- MySQL: order lifecycle + outbox + dedup.
- Kafka: consumes payment/inventory events and emits order events via outbox.
- Redis: not required in order service currently.

## Flow
1. `POST /` creates `OrderRecord` with initial state.
2. Order event is inserted into `OutboxEventRecord`.
3. Scheduled outbox publisher sends Kafka events and marks outbox state.
4. Kafka consumers update order status idempotently using `ConsumedEventRecord`.
5. Timeout scheduler moves stale `PAYMENT_PENDING` orders to timeout flow.
