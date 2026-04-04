# Edge Deployment Plan

Last updated: 2026-03-30

## Chosen Direction

- Frontend: Vercel
- Backend edge: Cloudflare
- Product database: MongoDB Atlas free tier
- Relational database target: Supabase Postgres

## Important Constraint

The current backend is a Spring Boot microservices system. Cloudflare Workers cannot run these JVM services directly.

That means there are two realistic deployment shapes:

1. Cloudflare as edge/proxy in front of a JVM-capable backend
- Keep the current Spring Boot services.
- Deploy the Java services to a platform that can run containers or JVM processes.
- Put Cloudflare in front for DNS, caching, WAF, rate limits, and routing.

2. Cloudflare Workers as the backend runtime
- Rewrite the backend API layer for the Workers runtime.
- This is a product and architecture change, not just a deployment change.

## Recommended Near-Term Setup

- Deploy `ecom-storefront` to Vercel.
- Move `product-service` Mongo usage to MongoDB Atlas via `PRODUCT_MONGODB_URI`.
- Replace the current MySQL production target with Supabase Postgres for relational services.
- Keep Cloudflare at the edge for the public domain and API routing.
- Run the existing Spring Boot backend on a JVM-capable host until there is a deliberate rewrite plan.

## Migration Note

The repo is still implemented and documented primarily around MySQL for relational services. Choosing Supabase means we now need an explicit MySQL-to-Postgres migration track:

1. Update Spring datasource and environment configuration for Postgres.
2. Review Liquibase changesets, SQL syntax, column types, and indexes for Postgres compatibility.
3. Re-run integration tests against Postgres instead of MySQL where relevant.
4. Update architecture and progress docs once the code migration is complete.

## Immediate Follow-Up

1. Add Vercel environment variables for the storefront.
2. Add Supabase connection settings and secrets for backend services.
3. Migrate relational-service config and schema assumptions from MySQL to Postgres.
4. Choose the JVM-capable backend host for the current services.
5. Put Cloudflare in front of the API and storefront domains.
6. Keep a separate Worker/BFF rewrite decision as a later optimization, not part of beta launch.
