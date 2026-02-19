# API Docs - Search Service

Base path: `/api/search`

## Endpoints
- `POST /index/products` - upsert one search document.
- `POST /index/products/bulk` - upsert many documents.
- `DELETE /index/products/{productId}` - remove document.
- `GET /products` - ranked product search.
- `GET /autocomplete` - suggestion query.
- `POST /reindex/products` - pull product pages and reindex.
- `GET /admin/relevance/evaluate` - evaluate relevance dataset quality.
- `GET /admin/relevance/dataset/health` - evaluate dataset refresh cadence.

## Entities
- `SearchProductDocument` (Elasticsearch index)
- `ConsumedEventRecord` (Elasticsearch index)
- Relevance dataset resources (`search-relevance-dataset*.json`)

## Data Stores
- Elasticsearch: search documents + dedup records.
- Kafka: consumes product indexing events.
- Redis/MySQL: not engaged for search persistence.

## Flow
1. Index APIs write `SearchProductDocument` into Elasticsearch.
2. Search API builds boosted DSL query and returns ranked results.
3. Reindex API pulls product pages from product service and bulk indexes.
4. Relevance endpoints evaluate dataset pass-rate and freshness metadata.
5. Dedup cleanup scheduler removes old consumed-event records.
