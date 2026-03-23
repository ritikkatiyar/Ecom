# Hostinger VPS Deployment

This repo is not ready for shared hosting. Use a Hostinger VPS.

## Production slice

Default production compose starts:
- `nginx`
- `storefront`
- `gateway`
- `auth-service`
- `user-service`
- `product-service`
- `inventory-service`
- `cart-service`
- `mysql`
- `mongo`
- `redis`
- `kafka`

Optional checkout services:
- `order-service`
- `payment-service`

Enable them with:

```powershell
docker compose -f docker-compose.prod.yml --profile checkout up -d --build
```

## First-time setup

1. Copy env template:

```powershell
Copy-Item .env.prod.example .env.prod
```

2. Fill all required secrets in `.env.prod`.
   `NEXT_PUBLIC_BACKEND_URL` must point at the gateway inside the compose network, which is already set to `http://gateway:8080` in the template.
   Set `NGINX_SERVER_NAME` to your public domain or `_` until DNS is ready.

3. Start the base stack:

```powershell
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

4. Start checkout too when needed:

```powershell
docker compose --env-file .env.prod -f docker-compose.prod.yml --profile checkout up -d --build
```

5. Run the public smoke checks:

```powershell
python .\ecom-back\scripts\check_hostinger_prod_smoke.py --base-url http://127.0.0.1
```

## Notes

- `application.yml` contains only shared settings.
- Production services must run with `SPRING_PROFILES_ACTIVE=prod`.
- Internal service-to-service URLs use Docker DNS names such as `http://inventory-service:8084`.
- `nginx` is the only public entrypoint in the production compose file.
- `NGINX_SERVER_NAME` is env-driven, so the same compose file can be used before and after DNS cutover.
- Nginx now includes a simple `/healthz` endpoint, baseline security headers, gzip, and cache headers for `/_next/static/*`.
- TLS is still not included. Terminate HTTPS at Nginx or an upstream proxy before public launch.
- Search, review, notification, and observability are intentionally excluded from the initial Hostinger slice.
- The smoke script verifies the public edge only: Nginx health, home page, frontend flags, and product listing.
