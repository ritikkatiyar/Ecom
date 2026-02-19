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
- `STAGING_ROLLBACK_WEBHOOK` (recommended; if missing, workflow records `rollbackState=skipped_missing_webhook`)
- `STAGING_ROLLBACK_VERIFY_WEBHOOK` (recommended; if missing, workflow records `rollbackCallbackState=skipped_missing_webhook`)
- `STAGING_RELEASE_GATE_METRICS_URL` (optional but recommended; points to gateway `/internal/release-gate/callbacks`)

### Production
- `PRODUCTION_DEPLOY_WEBHOOK`
- `PRODUCTION_SMOKE_URLS` (comma-separated health URLs)
- `PRODUCTION_SMOKE_BEARER_TOKEN` (optional)
- `PRODUCTION_ROLLBACK_WEBHOOK`
- `PRODUCTION_ROLLBACK_VERIFY_WEBHOOK`
- `PRODUCTION_RELEASE_GATE_METRICS_URL` (optional but recommended; points to gateway `/internal/release-gate/callbacks`)

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
- Rollback verification callback failure should fail release gate and be visible in `GITHUB_STEP_SUMMARY`.
- Workflow uploads `staging-release-gate-report` and `production-release-gate-report` artifacts with smoke/rollback outcomes.
- `ReleaseGateRollbackCallbackFailuresPresent` (warning): >=1 callback failure in 6h.
- `ReleaseGateRollbackCallbackFailureRateCritical` (critical): failure rate >20% over 24h with >=3 callback events.
- `ReleaseGateRollbackCallbackDrillMissing` (warning): no callback events seen in 14 days.

## Calibration Baseline (Current)
- Failure presence window: `6h` for quick drill visibility.
- Critical failure-rate window: `24h`, threshold `20%`, minimum volume `3` events.
- Drill cadence signal: at least `1` callback event every `14` days.

## Drill Evaluation + Delta Capture
Use release-gate artifacts to evaluate staged/prod rollback drill outcomes and produce tuning deltas:

```powershell
python ecom-back/scripts/evaluate_release_gate_drills.py `
  --staging build-artifacts/staging-release-gate.json `
  --production build-artifacts/production-release-gate.json `
  --out-dir build-artifacts
```

Outputs:
- `build-artifacts/release-gate-drill-delta-report.json`
- `build-artifacts/release-gate-drill-delta-report.md`
- `build-artifacts/release-gate-drill-log.md` (history rows for runbook table fields)

### Scheduled Drill Workflow
- Workflow: `.github/workflows/release-gate-drill.yml`
- Purpose: execute staged + production rollback/callback drills and generate delta report automatically.
- Schedule: weekly Thursday (`11:00 UTC`) + manual trigger.
- Script entrypoint: `ecom-back/scripts/run_release_gate_drill.py`

### Runbook Log Template
| Date (UTC) | Staging Status | Production Status | Delta Applied | Owner | Notes |
|---|---|---|---|---|---|
| YYYY-MM-DD | drill_passed / attention_required / missing | drill_passed / attention_required / missing | yes/no + setting | oncall-handle | artifact link + rationale |

Automated log command:
```powershell
python ecom-back/scripts/record_release_gate_drill_log.py `
  --delta-report build-artifacts/release-gate-drill-delta-report.json `
  --owner platform-oncall `
  --out build-artifacts/release-gate-drill-log.md
```

### Readiness Checklist Gate
- Workflow: `.github/workflows/release-readiness-checklist.yml`
- Purpose: fail readiness when drill/calibration evidence is missing or unhealthy.
- Required artifacts for pass:
  - receiver validation report
  - ops receiver drill report
  - load budget calibration report
  - release-gate drill delta report (staging + production not `missing`/`attention_required`)
