# Release ASAP Checklist

## Pre-Release
- [ ] `main` branch green on `backend-quality` and `backend-release`.
- [ ] Frontend beta banner enabled.
- [ ] Admin console feature flag gated.
- [ ] Smoke URLs configured for staging.
- [ ] Staging rollback webhook + verification webhook configured.

## Release Day
- [ ] Deploy backend to staging.
- [ ] Run staging smoke checks.
- [ ] Verify rollback callback metrics in gateway.
- [ ] Deploy frontend beta (`ecom-storefront`).
- [ ] Validate browse/search/cart happy path manually.

## Post-Release (First 48h)
- [ ] Monitor 5xx rate, p95 latency, and auth failures.
- [ ] Monitor cart add/remove success rate.
- [ ] Capture top 10 user feedback issues.
- [ ] Patch critical issues via feature flags or rollback.
