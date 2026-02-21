# Public Beta Launch Plan

Last updated: 2026-02-21

## Goal
Release a public beta quickly, get real traffic, and continue hardening in parallel.

## Track A - Frontend Release (ASAP)
1. Day 0-1 ✅ (partial)
- [x] Ship beta-ready frontend shell: `ecom-storefront` (Next.js) with stitch design.
- [x] Routes: home, shop, products/[id], search, cart, account, collections.
- [ ] Wire product/search/cart core APIs through gateway. (Products admin CRUD + image upload wired.)
- [ ] Add feature flags and admin route guard.

2. Day 2-3
- Add onboarding/beta messaging and feedback capture.
- Enable production analytics + funnel tracking.

3. Day 4-5
- Public beta rollout with controlled traffic.
- Monitor latency/error/conversion dashboards daily.

## Track B - Backend Hardening (Parallel)
1. Keep release-gate drill workflow active until staged/prod evidence is healthy.
2. Complete production receiver validation and rollback callback coverage.
3. Tune thresholds from live traffic and keep release gates strict.

## Deployment Stages
1. Internal dogfood (team only)
2. Private beta (invite users)
3. Public beta (open signup)
4. Stable v1

## Exit Criteria for Public Beta
- Core browse/search/cart/order flows are working.
- Frontend feature flags are active.
- Observability dashboards and alerts are live.
- Rollback trigger path is validated in staging.
