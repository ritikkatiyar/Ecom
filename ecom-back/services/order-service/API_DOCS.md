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
1. `OrderController` delegates to `OrderUseCases` (`OrderService`) for orchestration.
2. `POST /` creates `OrderRecord` with initial `CREATED` state.
3. `OrderItemCodec` serializes line items into `itemsJson` and `OrderEventPublisher` enqueues `order.created.v1` into `OutboxEventRecord`.
4. `OrderService` transitions order to `PAYMENT_PENDING`; `OrderResponseMapper` maps persistence model to API DTO.
5. Scheduled outbox publisher sends Kafka events and marks outbox state.
6. Kafka consumers update order status idempotently using `ConsumedEventRecord`.
7. Timeout scheduler moves stale `PAYMENT_PENDING` orders to `CANCELLED` and publishes `order.timed-out.v1`.
