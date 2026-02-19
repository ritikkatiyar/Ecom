# API Docs - Payment Service

Base path: `/api/payments`

## Endpoints
- `POST /intents` - create payment intent.
- `GET /{paymentId}` - fetch payment status.
- `POST /webhook` - provider callback (idempotent, HMAC signature required in `X-Razorpay-Signature`).
- `GET /provider/outage-mode` - read simulated provider outage mode.
- `POST /provider/outage-mode?enabled=true|false` - toggle outage mode for drills.
- `GET /provider/dead-letters` - list provider DLQ records.
- `POST /provider/dead-letters/{id}/requeue` - retry a provider DLQ record.

## Entities
- `PaymentRecord` (MySQL)
- `WebhookEventRecord` (MySQL)
- `ProviderDeadLetterRecord` (MySQL)
- `OutboxEventRecord` (MySQL)
- `ConsumedEventRecord` (MySQL)

## Data Stores
- MySQL: payment, webhook idempotency, provider DLQ, outbox, dedup.
- Kafka: consumes `order.created`; publishes payment result events via outbox.
- Redis: not used in payment service currently.

## Flow
1. Intent API creates/updates `PaymentRecord`.
2. Provider adapter is called with retry policy (`app.payment.provider.max-attempts`).
3. Provider outage/transient failures after max retries move request to `ProviderDeadLetterRecord` and increment DLQ metrics.
4. Requeue endpoint retries a DLQ record and recreates `PaymentRecord` when provider recovers.
5. Webhook API validates `X-Razorpay-Signature` using HMAC-SHA256 over raw payload; invalid/missing signature is rejected.
6. Webhook payload is validated and deduplicated using `WebhookEventRecord`.
7. Payment state changes create outbox events in `OutboxEventRecord`.
8. Outbox publisher emits `payment.authorized`/`payment.failed` to Kafka.
9. `ConsumedEventRecord` dedup table prevents duplicate processing for Kafka consumers.
