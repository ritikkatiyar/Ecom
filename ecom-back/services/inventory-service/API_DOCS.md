# API Docs - Inventory Service

Base path: `/api/inventory`

## Endpoints
- `POST /stock` - upsert stock for SKU.
- `GET /stock/{sku}` - read stock counters.
- `POST /reserve` - create reservation and reserve quantity.
- `POST /release` - release reserved quantity.
- `POST /confirm` - confirm reservation and consume reserved quantity.

## Entities
- `InventoryStock` (MySQL)
- `InventoryReservation` (MySQL)
- `OutboxEventRecord` (MySQL)
- `ConsumedEventRecord` (MySQL)

## Data Stores
- MySQL: stock, reservations, outbox, dedup tables.
- Redis: distributed SKU lock.
- Kafka: consumes saga events (`order.created`, `payment.authorized`, `payment.failed`, `order.timed-out`); publishes inventory reservation outcomes via outbox.

## Flow
1. Reserve/release/confirm APIs lock SKU via Redis.
2. Stock counters are updated in `InventoryStock` under DB transaction.
3. Reservation state transitions are written in `InventoryReservation`.
4. Saga consumers call order-scoped reserve/confirm/release methods.
5. Outbox publisher emits Kafka events asynchronously.
6. Expiry scheduler periodically releases stale `RESERVED` reservations.
7. Flash-sale SLO verification is covered by `ecom-back/load-tests/k6/flash-sale-inventory.js` (k6 thresholds + oversell invariant check).
