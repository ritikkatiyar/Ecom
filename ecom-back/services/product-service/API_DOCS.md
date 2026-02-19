# API Docs - Product Service

Base path: `/api/products`

## Endpoints
- `POST /` - create product.
- `PUT /{id}` - update product.
- `GET /{id}` - fetch product by id.
- `DELETE /{id}` - delete product.
- `GET /` - list/search products with pagination/filter/sort.

## Entities
- `Product` (MongoDB document)

## Data Stores
- MongoDB: canonical product catalog store.
- Kafka: producer/consumer contracts for product indexing events are planned.
- Redis: not used in product service currently.

## Flow
1. API receives product CRUD/list request.
2. Request is validated and mapped to/from `Product`.
3. Reads/writes go to MongoDB.
4. Search indexing sync currently runs via search service reindex endpoints.
