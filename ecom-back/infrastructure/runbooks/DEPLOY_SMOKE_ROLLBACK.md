# Deploy Smoke + Rollback Runbook

## Purpose
Automate post-deploy validation and rollback trigger in GitHub Actions release pipeline (`.github/workflows/backend-release.yml`).

## Flow
1. Deploy to `staging` or `production` via deploy webhook.
2. Run smoke probes using `ecom-back/scripts/post_deploy_smoke.py`.
3. If smoke fails, trigger rollback webhook using `ecom-back/scripts/trigger_rollback.py`.
4. Fail workflow job so promotion gate remains blocked.

## Required GitHub Environment Secrets

### Staging
- `STAGING_DEPLOY_WEBHOOK`
- `STAGING_SMOKE_URLS` (comma-separated health URLs)
- `STAGING_SMOKE_BEARER_TOKEN` (optional)
- `STAGING_ROLLBACK_WEBHOOK`

### Production
- `PRODUCTION_DEPLOY_WEBHOOK`
- `PRODUCTION_SMOKE_URLS` (comma-separated health URLs)
- `PRODUCTION_SMOKE_BEARER_TOKEN` (optional)
- `PRODUCTION_ROLLBACK_WEBHOOK`

## Suggested Smoke URLs
- API gateway health endpoint
- Auth service health endpoint
- Inventory service health endpoint
- Payment service health endpoint

Example:
`https://staging-api.example.com/actuator/health,https://staging-payment.example.com/actuator/health`

## Expected Alerts
- Failed smoke + rollback trigger should produce CI failure signal.
- If rollback webhook fails, workflow should fail and on-call should be paged.
