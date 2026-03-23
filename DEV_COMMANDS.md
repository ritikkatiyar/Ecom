# Dev Commands

Run these from `d:\ecom`.

## Important (Windows)

Use a VS Code terminal running as **Administrator** and launch with `ExecutionPolicy Bypass` to avoid Docker access and script policy issues.

## Single script for everything

```powershell
.\run-side-by-side.ps1 -Action start
```

This now starts the lighter day-to-day stack by default:
- Infra: MySQL, Kafka (KRaft mode, no ZooKeeper)
- Apps: storefront, gateway, auth, user, product, inventory, cart
- Not started unless requested: Redis, order, payment, review, notification, Kafka UI, Elasticsearch, `search-service`, Zipkin, Prometheus, Alertmanager, Grafana

Why this is the default:
- The repo launches many Spring Boot services, and each Java process adds noticeable RAM pressure.
- On this machine, the live process list already showed several Java services in the `70-150 MB` working-set range each, plus Docker Desktop overhead.
- Search and observability are the biggest avoidable extras. Checkout and reviews are also easy to keep off unless you are actively working on them.
- Cart and inventory now have in-memory fallback for local development when Redis is not running.
- Kafka now runs in single-node KRaft mode locally, so ZooKeeper is no longer part of the dev stack.
- Kafka UI is available as an optional container on `http://127.0.0.1:8091`.
- `product-service` now prefers `PRODUCT_MONGODB_URI` from `D:\ecom\.env.local`. If that env var is set, Docker MongoDB is skipped entirely.
- If `PRODUCT_MONGODB_URI` is not set, the launcher falls back to Docker MongoDB on `127.0.0.1:27018`.

## Use cloud MongoDB in dev

Add this to `D:\ecom\.env.local`:

```env
PRODUCT_MONGODB_URI=mongodb+srv://<user>:<password>@<cluster>/<database>?retryWrites=true&w=majority
```

Then run:

```powershell
.\run-side-by-side.ps1 -Action restart
```

## Start full project (bypass execution policy)

```powershell
powershell -ExecutionPolicy Bypass -Command "& '.\run-side-by-side.ps1' -Action start"
```

## Stop full project (jobs + infra)

```powershell
.\run-side-by-side.ps1 -Action stop
```

## Restart full project

```powershell
.\run-side-by-side.ps1 -Action restart
```

The launcher passes the Spring `dev` profile explicitly for backend services. For non-dev environments, set `SPRING_PROFILES_ACTIVE=prod` in the host or container environment instead of committing an active profile to source.
Old `build-artifacts/*.log` files are pruned automatically after `7` days. Override with `-LogRetentionDays <n>`.

## Show current status

```powershell
.\run-side-by-side.ps1 -Action status
```

## Recommended daily flow (stable)

```powershell
.\run-side-by-side.ps1 -Action restart
.\run-side-by-side.ps1 -Action status
```

## Start checkout flow too

```powershell
.\run-side-by-side.ps1 -Action start -EnableCheckout:$true
```

## Start with Redis too

```powershell
.\run-side-by-side.ps1 -Action start -EnableRedis:$true
```

## Start reviews too

```powershell
.\run-side-by-side.ps1 -Action start -EnableReviews:$true
```

## Start notifications too

```powershell
.\run-side-by-side.ps1 -Action start -EnableNotifications:$true
```

## Start with search enabled

```powershell
.\run-side-by-side.ps1 -Action start -EnableSearch:$true
```

## Start with Kafka UI enabled

```powershell
.\run-side-by-side.ps1 -Action start -EnableKafkaUi:$true
```

## Start with full observability stack too

```powershell
.\run-side-by-side.ps1 -Action start -EnableRedis:$true -EnableCheckout:$true -EnableNotifications:$true -EnableSearch:$true -EnableObservability:$true
```

## Start while skipping Cloudinary env validation

```powershell
.\run-side-by-side.ps1 -Action start -SkipCloudinaryCheck:$true
```

## Full local API smoke suite

```powershell
python .\ecom-back\scripts\check_local_api_smoke.py
```
