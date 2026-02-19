# Release Gate Drill Delta Report

Generated at: 2026-02-19T16:39:49.136937+00:00

| Environment | Status | Smoke | Rollback Trigger | Rollback Callback | Notes |
|---|---|---|---|---|---|
| staging | missing | missing | missing | missing | artifact_missing |
| production | missing | missing | missing | missing | artifact_missing |

## Recommended Tuning Deltas
- Setting: `ReleaseGateRollbackCallbackDrillMissing window`
  - Current: `14d`
  - Recommended: `Keep 14d and schedule explicit staged/prod rollback drills this cycle.`
  - Reason: `Rollback path not exercised in both environments.`
