# Event-Driven Architecture (EDA) in Ecom Backend

Last updated: 2025-02-21

## Overview

The backend uses an **event-driven architecture** with **Apache Kafka** as the message broker. Services communicate asynchronously via domain events, enabling loose coupling, scalability, and resilience.

---

## Core Components

### 1. **Domain Event Structure**

All events follow a common envelope defined by `DomainEvent<T>`:

```
DomainEvent<T>
├── eventId      (UUID) – idempotency / deduplication
├── eventType    (string) – e.g. "order.created.v1"
├── occurredAt   (Instant)
├── producer     (string) – e.g. "order-service"
├── schemaVersion (string) – "v1"
├── traceId      (string) – distributed tracing
└── payload      (T) – typed event-specific data
```

### 2. **Outbox Pattern (Transactional Outbox)**

Publishers do **not** send to Kafka directly. Instead:

1. **Enqueue**: Business logic writes an event to an `OutboxEventRecord` table in the **same transaction** as the domain change.
2. **Poll & Publish**: A scheduled `OutboxPublisher` polls `PENDING` records and sends them to Kafka.
3. **Retry**: Failed sends are retried; after max attempts they move to `FAILED` and can be replayed manually.

This guarantees **at-least-once delivery** and avoids dual-write failures.

### 3. **Consumer Deduplication**

Consumers store `eventId` in `ConsumedEventRecord` before processing. If the same event is delivered again (Kafka at-least-once), it is skipped, ensuring **idempotent handling**.

---

## Event Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                              KAFKA BROKER (Topics)                                       │
│  order.created.v1 │ payment.authorized.v1 │ payment.failed.v1 │ order.timed-out.v1       │
│  inventory.reserved.v1 │ inventory.reservation.failed.v1 │ product.upserted.v1 │ ...     │
└─────────────────────────────────────────────────────────────────────────────────────────┘
         ▲                    ▲                    ▲                    ▲
         │                    │                    │                    │
    ┌────┴────┐          ┌────┴────┐          ┌────┴────┐          ┌────┴────┐
    │ Outbox  │          │ Outbox  │          │ Outbox  │          │ Outbox  │
    │ Publisher│          │ Publisher│          │ Publisher│          │ Publisher│
    └────┬────┘          └────┬────┘          └────┬────┘          └────┬────┘
         │                    │                    │                    │
┌────────┴────────────────────┴────────────────────┴────────────────────┴────────────────┐
│                                    SERVICES                                            │
│  Order Service  │  Payment Service  │  Inventory Service  │  Product Service  │  ...    │
│  (Outbox + DB)  │  (Outbox + DB)    │  (Outbox + DB)      │  (Outbox + DB)    │         │
└────────────────────────────────────────────────────────────────────────────────────────┘
         │                    │                    │                    │
         │              @KafkaListener         @KafkaListener         @KafkaListener
         ▼                    ▼                    ▼                    ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                           CONSUMERS (by service)                                        │
│  PaymentResultConsumer  │  InventorySagaConsumer  │  OrderEventConsumer  │  ...         │
│  InventoryReservationConsumer  │  ProductIndexConsumer  │  (ConsumedEventRecord dedup)  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Event Contracts (Producer → Consumers)

| Event | Producer | Consumers |
|-------|----------|-----------|
| `order.created.v1` | Order Service | Payment, Inventory, Notification |
| `order.timed-out.v1` | Order Service | Inventory |
| `payment.authorized.v1` | Payment Service | Order, Inventory, Notification |
| `payment.failed.v1` | Payment Service | Order, Inventory, Notification |
| `inventory.reserved.v1` | Inventory Service | (none) |
| `inventory.reservation.failed.v1` | Inventory Service | Order |
| `product.upserted.v1` | Product Service | Search |
| `product.deleted.v1` | Product Service | Search |
| `notification.alert.v1` | Notification Service | (ops) |
| `notification.dlq.v1` | Notification Service | (ops) |

---

## Order Saga Flow (Main Use Case)

The order lifecycle is orchestrated via events (choreography, not central orchestrator):

```
                    ┌──────────────────┐
                    │  Order Service   │
                    │  Create Order    │
                    └────────┬─────────┘
                             │
                             │ order.created.v1 (via Outbox)
                             ▼
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   Payment     │   │   Inventory   │   │  Notification │
│   Service     │   │   Service     │   │   Service     │
│ reserve $     │   │ reserve stock │   │ send email    │
└───────┬───────┘   └───────┬───────┘   └───────────────┘
        │                   │
        │ payment.authorized / payment.failed
        ▼                   │
┌───────────────┐           │
│ Order Service │◄──────────┘
│ update status │   inventory.reservation.failed (if reserve fails)
└───────────────┘
        │
        │ order.timed-out.v1 (scheduler, if payment times out)
        ▼
┌───────────────┐
│   Inventory   │
│ release stock │
└───────────────┘
```

**Flow in words:**

1. **Order created** → `order.created.v1` published.
2. **Payment** consumes → charges card → publishes `payment.authorized.v1` or `payment.failed.v1`.
3. **Inventory** consumes `order.created` → reserves stock → publishes `inventory.reserved.v1` or `inventory.reservation.failed.v1`.
4. **Inventory** consumes `payment.authorized` → confirms reservation; consumes `payment.failed` or `order.timed-out` → releases reservation.
5. **Order** consumes `payment.authorized`/`payment.failed` → updates status; consumes `inventory.reservation.failed` → marks order as failed.
6. **Notification** consumes order/payment events → sends emails or alerts.

---

## Product → Search Flow

```
Product Service                    Search Service
      │                                  │
      │ product.upserted.v1              │
      │ product.deleted.v1               │
      └─────────────────────────────────►  ProductIndexConsumer
                                          │
                                          ▼
                                    update / remove
                                    from search index
```

---

## Reliability Mechanisms

| Mechanism | Purpose |
|-----------|--------|
| **Transactional Outbox** | Ensure events are published only after DB commit; avoid lost events |
| **ConsumedEventRecord** | Idempotent consumption; skip duplicate deliveries |
| **Outbox retry + FAILED** | Handle transient Kafka errors; manual replay for persistent failures |
| **Dead Letter Queue (DLQ)** | Capture notification failures for inspection / replay |
| **Event contracts (JSON Schema)** | Validate payloads; `check_event_contracts.py` for CI |

---

## Kafka Topics (typical naming)

- `order.created.v1`
- `order.timed-out.v1`
- `payment.authorized.v1`
- `payment.failed.v1`
- `inventory.reserved.v1`
- `inventory.reservation.failed.v1`
- `product.upserted.v1`
- `product.deleted.v1`
- `notification.alert.v1`
- `notification.dlq.v1`

Topic names are configurable via `app.kafka.topics.*` in `application.yml` per service.

---

## Diagram (Mermaid)

```mermaid
flowchart TB
    subgraph Kafka["Apache Kafka"]
        T1[order.created.v1]
        T2[payment.authorized.v1]
        T3[payment.failed.v1]
        T4[order.timed-out.v1]
        T5[inventory.reserved.v1]
        T6[inventory.reservation.failed.v1]
        T7[product.upserted.v1]
        T8[product.deleted.v1]
    end

    subgraph Order["Order Service"]
        O_Order[Create Order]
        O_Outbox[Outbox Table]
        O_Pub[OutboxPublisher]
        O_PayCons[PaymentResultConsumer]
        O_InvCons[InventoryReservationConsumer]
    end

    subgraph Payment["Payment Service"]
        P_Cons[OrderCreatedConsumer]
        P_Process[Process Payment]
        P_Outbox[Outbox Table]
        P_Pub[OutboxPublisher]
    end

    subgraph Inventory["Inventory Service"]
        I_Cons[InventorySagaConsumer]
        I_Reserve[Reserve/Release Stock]
        I_Outbox[Outbox Table]
        I_Pub[OutboxPublisher]
    end

    subgraph Notification["Notification Service"]
        N_Cons[OrderEventConsumer]
        N_Send[Send Notifications]
    end

    subgraph Product["Product Service"]
        Prod_Catalog[Product Catalog]
    end

    subgraph Search["Search Service"]
        S_Cons[ProductIndexConsumer]
        S_Index[Search Index]
    end

    O_Order --> O_Outbox
    O_Outbox --> O_Pub
    O_Pub --> T1
    T1 --> P_Cons
    T1 --> I_Cons
    T1 --> N_Cons

    P_Cons --> P_Process
    P_Process --> P_Outbox
    P_Outbox --> P_Pub
    P_Pub --> T2
    P_Pub --> T3

    T2 --> O_PayCons
    T2 --> I_Cons
    T2 --> N_Cons
    T3 --> O_PayCons
    T3 --> I_Cons
    T3 --> N_Cons

    I_Cons --> I_Reserve
    I_Reserve --> I_Outbox
    I_Outbox --> I_Pub
    I_Pub --> T5
    I_Pub --> T6

    T4 --> I_Cons
    O_Pub --> T4
    T6 --> O_InvCons

    Prod_Catalog -.->|contract| T7
    Prod_Catalog -.->|contract| T8
    T7 --> S_Cons
    T8 --> S_Cons
    S_Cons --> S_Index

    N_Cons --> N_Send
```

---

## References

- Event contracts: `ecom-back/contracts/events/`
- Domain event model: `common/common-events/src/main/java/com/ecom/common/DomainEvent.java`
- Outbox support: `common/common-core/src/main/java/com/ecom/common/reliability/OutboxPublishSupport.java`
- Contract validation: `ecom-back/scripts/check_event_contracts.py`

---

## Order to Kafka Flow (File-by-File)

This is the exact publish path used by `order-service`.

1. API receives create-order request  
   File: `services/order-service/src/main/java/com/ecom/order/controller/OrderController.java`

2. Service creates order and triggers event publishing  
   Method: `createOrder(...)`  
   File: `services/order-service/src/main/java/com/ecom/order/service/OrderService.java`

3. Event payload object is built for order-created  
   Method: `publishOrderCreated(...)`  
   File: `services/order-service/src/main/java/com/ecom/order/service/OrderEventPublisher.java`  
   Payload type: `services/order-service/src/main/java/com/ecom/order/service/OrderCreatedPayload.java`

4. Event is enqueued in outbox (not directly sent to Kafka)  
   Method: `enqueue(...)`  
   File: `services/order-service/src/main/java/com/ecom/order/service/OutboxService.java`

5. Standard envelope is created and serialized  
   Envelope type: `common/common-events/src/main/java/com/ecom/common/DomainEvent.java`

6. Outbox row is saved as `PENDING` in `order_outbox_events`  
   Entity: `services/order-service/src/main/java/com/ecom/order/entity/OutboxEventRecord.java`  
   Repository: `services/order-service/src/main/java/com/ecom/order/repository/OutboxEventRepository.java`

7. Scheduled publisher polls pending outbox rows  
   Method: `publishPending()` runs every 3 seconds  
   File: `services/order-service/src/main/java/com/ecom/order/service/OutboxPublisher.java`

8. Kafka send happens here  
   Call: `kafkaTemplate.send(topic, messageKey, payload).get(...)`  
   File: `common/common-core/src/main/java/com/ecom/common/reliability/OutboxPublishSupport.java`

9. Send result updates outbox status  
   - Success -> `SENT`  
   - Failure -> increment attempts, keep `PENDING` or mark `FAILED` after max retry  
   File: `common/common-core/src/main/java/com/ecom/common/reliability/OutboxPublishSupport.java`

10. Runtime topic/retry config used by order-service  
   File: `services/order-service/src/main/resources/application.yml`  
   Keys:
   - `app.kafka.topics.order-created`
   - `app.kafka.topics.order-timed-out`
   - `app.outbox.max-retry`

11. Same pipeline used for timeout event publish (`order.timed-out.v1`)  
   Producer method: `publishOrderTimedOut(...)`  
   File: `services/order-service/src/main/java/com/ecom/order/service/OrderEventPublisher.java`

12. Example downstream consumers for `order.created.v1`  
   - Inventory: `services/inventory-service/src/main/java/com/ecom/inventory/kafka/InventorySagaConsumer.java`
   - Payment: `services/payment-service/src/main/java/com/ecom/payment/kafka/OrderCreatedConsumer.java`
