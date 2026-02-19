#!/usr/bin/env python3
"""
Calibrate load-test budget recommendations from observed production metrics.

Inputs are provided by environment variables so values can come from CI variables,
dashboard exports, or manual run overrides.
"""

from __future__ import annotations

import json
import os
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


@dataclass
class BudgetPair:
    current: float
    observed: float
    kind: str  # "max" or "min"
    margin: float
    floor: float | None = None
    ceil: float | None = None

    def recommended(self) -> float:
        if self.kind == "max":
            candidate = max(self.current, self.observed * self.margin)
        else:
            candidate = min(self.current, self.observed * self.margin)

        if self.floor is not None:
            candidate = max(candidate, self.floor)
        if self.ceil is not None:
            candidate = min(candidate, self.ceil)
        return round(candidate, 4)

    def status(self) -> str:
        rec = self.recommended()
        if abs(rec - self.current) < 0.0001:
            return "unchanged"
        return "changed"


def env_float(name: str, default: float) -> float:
    raw = (os.getenv(name) or "").strip()
    if not raw:
        return default
    return float(raw)


def build_payload() -> dict:
    browse = {
        "p95_ms": BudgetPair(
            current=env_float("CURRENT_BROWSE_P95_MS", 260),
            observed=env_float("OBSERVED_BROWSE_P95_MS", 230),
            kind="max",
            margin=env_float("LATENCY_MARGIN_FACTOR", 1.2),
            floor=150,
            ceil=1200,
        ),
        "fail_rate_max": BudgetPair(
            current=env_float("CURRENT_BROWSE_FAIL_RATE_MAX", 0.03),
            observed=env_float("OBSERVED_BROWSE_FAIL_RATE", 0.015),
            kind="max",
            margin=env_float("ERROR_MARGIN_FACTOR", 1.5),
            floor=0.005,
            ceil=0.25,
        ),
    }

    cart = {
        "p95_ms": BudgetPair(
            current=env_float("CURRENT_CART_P95_MS", 300),
            observed=env_float("OBSERVED_CART_P95_MS", 260),
            kind="max",
            margin=env_float("LATENCY_MARGIN_FACTOR", 1.2),
            floor=180,
            ceil=1500,
        ),
        "fail_rate_max": BudgetPair(
            current=env_float("CURRENT_CART_FAIL_RATE_MAX", 0.04),
            observed=env_float("OBSERVED_CART_FAIL_RATE", 0.02),
            kind="max",
            margin=env_float("ERROR_MARGIN_FACTOR", 1.5),
            floor=0.005,
            ceil=0.25,
        ),
        "consistency_min": BudgetPair(
            current=env_float("CURRENT_CART_CONSISTENCY_MIN", 0.98),
            observed=env_float("OBSERVED_CART_CONSISTENCY", 0.991),
            kind="min",
            margin=env_float("CONSISTENCY_MARGIN_FACTOR", 0.995),
            floor=0.85,
            ceil=0.9999,
        ),
    }

    checkout = {
        "p95_ms": BudgetPair(
            current=env_float("CURRENT_CHECKOUT_P95_MS", 420),
            observed=env_float("OBSERVED_CHECKOUT_P95_MS", 360),
            kind="max",
            margin=env_float("LATENCY_MARGIN_FACTOR", 1.2),
            floor=220,
            ceil=1800,
        ),
        "fail_rate_max": BudgetPair(
            current=env_float("CURRENT_CHECKOUT_FAIL_RATE_MAX", 0.07),
            observed=env_float("OBSERVED_CHECKOUT_FAIL_RATE", 0.03),
            kind="max",
            margin=env_float("ERROR_MARGIN_FACTOR", 1.5),
            floor=0.01,
            ceil=0.35,
        ),
        "success_min": BudgetPair(
            current=env_float("CURRENT_CHECKOUT_SUCCESS_MIN", 0.90),
            observed=env_float("OBSERVED_CHECKOUT_SUCCESS", 0.965),
            kind="min",
            margin=env_float("SUCCESS_MARGIN_FACTOR", 0.99),
            floor=0.75,
            ceil=0.9999,
        ),
    }

    def to_metric(name: str, pair: BudgetPair) -> dict:
        return {
            "name": name,
            "current": pair.current,
            "observed": pair.observed,
            "recommended": pair.recommended(),
            "status": pair.status(),
        }

    suites = {
        "browse": [to_metric("p95_ms", browse["p95_ms"]), to_metric("fail_rate_max", browse["fail_rate_max"])],
        "cart": [
            to_metric("p95_ms", cart["p95_ms"]),
            to_metric("fail_rate_max", cart["fail_rate_max"]),
            to_metric("consistency_min", cart["consistency_min"]),
        ],
        "checkout": [
            to_metric("p95_ms", checkout["p95_ms"]),
            to_metric("fail_rate_max", checkout["fail_rate_max"]),
            to_metric("success_min", checkout["success_min"]),
        ],
    }

    changed = sum(1 for metrics in suites.values() for metric in metrics if metric["status"] == "changed")
    return {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "changedMetrics": changed,
        "suites": suites,
    }


def write_markdown(report: dict, md_path: Path) -> None:
    lines = [
        "# Production Load Budget Calibration Report",
        "",
        f"Generated at: {report['generatedAt']}",
        "",
        f"Changed metrics: {report['changedMetrics']}",
        "",
        "| Suite | Metric | Current | Observed | Recommended | Status |",
        "|---|---|---:|---:|---:|---|",
    ]

    for suite, metrics in report["suites"].items():
        for metric in metrics:
            lines.append(
                f"| {suite} | {metric['name']} | {metric['current']} | {metric['observed']} | "
                f"{metric['recommended']} | {metric['status']} |"
            )

    lines.extend(
        [
            "",
            "## Notes",
            "- Latency and fail-rate budgets are calibrated as max-thresholds with safety margins.",
            "- Consistency and success budgets are calibrated as minimum-thresholds with conservative margins.",
            "- Update release workflow thresholds after review if recommendation is acceptable.",
        ]
    )

    md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    report = build_payload()
    out_dir = Path(os.getenv("CALIBRATION_OUTPUT_DIR", "build-artifacts"))
    out_dir.mkdir(parents=True, exist_ok=True)

    json_path = out_dir / "load-budget-calibration-report.json"
    md_path = out_dir / "load-budget-calibration-report.md"

    json_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
    write_markdown(report, md_path)

    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
