# API Docs - Notification Service

Base path: `/api/notifications`

## Endpoints
- `GET /?userId=...` - list notifications.
- `GET /failed` - list failed notifications.
- `POST /retry-failed` - retry failed notifications.
- `GET /dead-letters` - list dead-letter records.
- `POST /dead-letters/{id}/requeue` - requeue dead-letter record.

## Entities
- `NotificationRecord` (MySQL)
- `NotificationDeadLetterRecord` (MySQL)
- `ConsumedEventRecord` (MySQL)

## Data Stores
- MySQL: notifications + dead-letter + dedup records.
- Kafka: consumes order/payment events; publishes DLQ/alert events.
- SMTP provider + log provider: delivery backends.

## Flow
1. Kafka consumers process order/payment events with dedup checks.
2. Notification payload is rendered through template service.
3. Delivery attempts update `NotificationRecord` status.
4. Retry scheduler retries failed notifications.
5. Exhausted retries move record to `NotificationDeadLetterRecord` and publish alert events.
