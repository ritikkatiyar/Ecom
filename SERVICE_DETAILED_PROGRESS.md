# Service Detailed Progress

Last updated: 2026-02-14

## API Gateway (`ecom-back/api-gateway`)
- APIs:
  - Route forwarding for `/api/auth`, `/api/products`, `/api/inventory`, `/api/cart`, `/api/orders`, `/api/payments`, `/api/search`.
- Kafka in/out:
  - None.
- Data model:
  - None.
- Patterns:
  - API gateway route aggregation.
  - Global correlation ID propagation (`X-Correlation-Id`).
  - API version guard (`X-API-Version: v1` for non-auth `/api/**` calls).
  - In-memory per-IP rate limiting (window-based).
  - JWT validation filter via auth-service `/api/auth/validate`.
- Verification:
  - `api-gateway` compiles with new filters and security config.
- Pending:
  - Distributed rate limiting (Redis/Bucket4j).
  - Route-level policy tuning and exception payload standardization.

## Auth Service (`ecom-back/services/auth-service`)
- APIs:
  - `POST /api/auth/signup`
  - `POST /api/auth/login`
  - `POST /api/auth/refresh`
  - `POST /api/auth/logout`
  - `GET /api/auth/validate`
- Kafka in/out:
  - None.
- Data model:
  - `UserAccount`, `RefreshToken` (MySQL).
  - Access-token blacklist (Redis).
- Patterns:
  - JWT + refresh token.
  - Token blacklist.
  - OAuth2 login handler.
- Verification:
  - Service compiles and runs in local stack.
- Pending:
  - Rotation/audit hardening.
  - Broader integration tests.

## User Service (`ecom-back/services/user-service`)
- APIs:
  - Health only.
- Kafka in/out:
  - None.
- Data model:
  - Not implemented.
- Patterns:
  - Scaffold only.
- Verification:
  - Module scaffold exists.
- Pending:
  - Full profile/address/preferences implementation.

## Product Service (`ecom-back/services/product-service`)
- APIs:
  - `POST /api/products`
  - `PUT /api/products/{id}`
  - `GET /api/products/{id}`
  - `DELETE /api/products/{id}`
  - `GET /api/products` (pagination/filter/sort/search)
- Kafka in/out:
  - No producer/consumer implemented yet for product index sync (currently search has direct indexing APIs and optional consumer).
- Data model:
  - `Product` (MongoDB).
- Patterns:
  - DIP via `ProductUseCases`.
  - OpenAPI + validation.
- Verification:
  - Compiles.
- Pending:
  - Product upsert/delete event publishing.
  - Variant/ranking-aware modeling enhancements.

## Inventory Service (`ecom-back/services/inventory-service`)
- APIs:
  - `POST /api/inventory/stock`
  - `GET /api/inventory/stock/{sku}`
  - `POST /api/inventory/reserve`
  - `POST /api/inventory/release`
  - `POST /api/inventory/confirm`
- Kafka in:
  - `order.created.v1`
  - `payment.authorized.v1`
  - `payment.failed.v1`
- Kafka out:
  - `inventory.reserved.v1`
  - `inventory.reservation.failed.v1`
- Data model:
  - `InventoryStock`, `InventoryReservation` (MySQL).
  - Redis lock for SKU operations.
- Patterns:
  - Lock-based stock reservation.
  - Saga participant with compensation.
  - Deterministic reservation IDs (`orderId:sku`) for reconciliation.
  - Consumer dedup for Kafka events.
  - Outbox publisher for inventory reservation outcome events.
- Verification:
  - Compiles after saga + outbox/dedup extension.
- Pending:
  - Reservation-expiry scheduler.
  - Contract and concurrency tests.

## Cart Service (`ecom-back/services/cart-service`)
- APIs:
  - `POST /api/cart/items`
  - `GET /api/cart`
  - `DELETE /api/cart/items/{productId}`
  - `DELETE /api/cart`
  - `POST /api/cart/merge`
- Kafka in/out:
  - None yet.
- Data model:
  - Guest cart in Redis.
  - User cart items in MySQL.
- Patterns:
  - Guest-to-user merge.
- Verification:
  - Service and tests compile.
- Pending:
  - Price snapshot validation.
  - Cart event publishing.

## Order Service (`ecom-back/services/order-service`)
- APIs:
  - `POST /api/orders`
  - `GET /api/orders/{orderId}`
  - `GET /api/orders?userId=...`
  - `POST /api/orders/{orderId}/cancel`
  - `POST /api/orders/{orderId}/confirm`
- Kafka in:
  - `payment.authorized.v1`
  - `payment.failed.v1`
  - `inventory.reservation.failed.v1`
- Kafka out:
  - `order.created.v1` (via outbox publisher)
- Data model:
  - `OrderRecord`, consumed-event dedup table, outbox table (MySQL).
- Patterns:
  - Saga coordinator role for order status.
  - Consumer idempotency.
  - Outbox publisher.
- Verification:
  - Compiles after outbox + saga compensation updates.
- Pending:
  - Replay tooling.
  - Timeout handling.

## Payment Service (`ecom-back/services/payment-service`)
- APIs:
  - `POST /api/payments/intents`
  - `GET /api/payments/{paymentId}`
  - `POST /api/payments/webhook`
- Kafka in:
  - `order.created.v1`
- Kafka out:
  - `payment.authorized.v1` (via outbox)
  - `payment.failed.v1` (via outbox)
- Data model:
  - `PaymentRecord`, `WebhookEventRecord`, consumed-event dedup table, outbox table (MySQL).
- Patterns:
  - Webhook idempotency.
  - Consumer idempotency.
  - Outbox publisher.
- Verification:
  - Compiles after outbox + dedup updates.
- Pending:
  - Provider signature hardening.
  - DLQ/retry policies for provider failures.

## Review Service (`ecom-back/services/review-service`)
- APIs:
  - Health only.
- Kafka in/out:
  - None.
- Data model:
  - Not implemented.
- Patterns:
  - Scaffold only.
- Verification:
  - Module scaffold exists.
- Pending:
  - CRUD and moderation implementation.

## Search Service (`ecom-back/services/search-service`)
- APIs:
  - `POST /api/search/index/products`
  - `POST /api/search/index/products/bulk`
  - `DELETE /api/search/index/products/{productId}`
  - `GET /api/search/products` (`activeOnly`, ranking-aware query)
  - `GET /api/search/autocomplete`
  - `POST /api/search/reindex/products`
- Kafka in:
  - `product.upserted.v1`
  - `product.deleted.v1`
- Kafka out:
  - None.
- Data model:
  - `SearchProductDocument` (Elasticsearch).
- Patterns:
  - Fuzzy multi-field query.
  - Boosted ranking for exact/phrase-prefix name matches.
  - Active-product filtering by default.
  - Autocomplete query.
  - Service-to-service reindex pull from product-service (paged import).
  - Consumer dedup for product index events.
  - DIP via `SearchUseCases`.
- Verification:
  - Compiles after ranking/reindex hardening updates.
- Pending:
  - Ranking calibration with production-like relevance samples.
  - Reindex and contract tests.

## Notification Service (`ecom-back/services/notification-service`)
- APIs:
  - `GET /api/notifications?userId=...`
  - `GET /api/notifications/failed`
  - `POST /api/notifications/retry-failed`
  - `GET /api/notifications/dead-letters`
  - `POST /api/notifications/dead-letters/{id}/requeue`
- Kafka in:
  - `order.created.v1`
  - `payment.authorized.v1`
  - `payment.failed.v1`
- Kafka out:
  - `notification.dlq.v1`
- Data model:
  - `NotificationRecord` (MySQL).
- Patterns:
  - Consumer dedup for notification event listeners.
  - Event idempotency.
  - Scheduled retry for failed deliveries.
  - Config-driven provider abstraction (`log` / `smtp`).
  - Dead-letter escalation on retry exhaustion with requeue workflow.
- Verification:
  - Compiles after SMTP provider and DLQ workflow updates.
- Pending:
  - Production SMTP credential/secrets wiring and template engine.
  - DLQ monitoring/alerts dashboard wiring.
