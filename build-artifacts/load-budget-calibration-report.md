# Production Load Budget Calibration Report

Generated at: 2026-02-19T16:10:50.076650+00:00

Changed metrics: 3

| Suite | Metric | Current | Observed | Recommended | Status |
|---|---|---:|---:|---:|---|
| browse | p95_ms | 260 | 230 | 276.0 | changed |
| browse | fail_rate_max | 0.03 | 0.015 | 0.03 | unchanged |
| cart | p95_ms | 300 | 260 | 312.0 | changed |
| cart | fail_rate_max | 0.04 | 0.02 | 0.04 | unchanged |
| cart | consistency_min | 0.98 | 0.991 | 0.98 | unchanged |
| checkout | p95_ms | 420 | 360 | 432.0 | changed |
| checkout | fail_rate_max | 0.07 | 0.03 | 0.07 | unchanged |
| checkout | success_min | 0.9 | 0.965 | 0.9 | unchanged |

## Notes
- Latency and fail-rate budgets are calibrated as max-thresholds with safety margins.
- Consistency and success budgets are calibrated as minimum-thresholds with conservative margins.
- Update release workflow thresholds after review if recommendation is acceptable.
