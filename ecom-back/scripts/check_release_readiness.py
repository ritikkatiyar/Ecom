#!/usr/bin/env python3
"""
Check release readiness from generated drill/calibration artifacts.
"""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path


def read_json(path: Path) -> dict | None:
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def gate(name: str, passed: bool, detail: str) -> dict:
    return {"name": name, "passed": passed, "detail": detail}


def main() -> int:
    out_dir = Path("build-artifacts")
    out_dir.mkdir(parents=True, exist_ok=True)

    receiver = read_json(out_dir / "alertmanager-receiver-validation-report.json")
    ops = read_json(out_dir / "ops-receiver-drill-report.json")
    calibration = read_json(out_dir / "load-budget-calibration-report.json")
    delta = read_json(out_dir / "release-gate-drill-delta-report.json")

    gates: list[dict] = []

    if receiver is None:
        gates.append(gate("receiver_validation", False, "missing alertmanager receiver validation report"))
    else:
        ok = receiver.get("status") == "passed"
        gates.append(gate("receiver_validation", ok, f"status={receiver.get('status', 'unknown')}"))

    if ops is None:
        gates.append(gate("ops_receiver_drill", False, "missing ops receiver drill report"))
    else:
        ok = ops.get("status") == "passed"
        gates.append(gate("ops_receiver_drill", ok, f"status={ops.get('status', 'unknown')}"))

    if calibration is None:
        gates.append(gate("load_budget_calibration", False, "missing load budget calibration report"))
    else:
        changed = calibration.get("changedMetrics", "unknown")
        gates.append(gate("load_budget_calibration", True, f"changedMetrics={changed}"))

    if delta is None:
        gates.append(gate("rollback_drill_delta", False, "missing release gate drill delta report"))
    else:
        staging = (delta.get("staging") or {}).get("status", "missing")
        production = (delta.get("production") or {}).get("status", "missing")
        ok = staging not in {"missing", "attention_required"} and production not in {"missing", "attention_required"}
        gates.append(
            gate(
                "rollback_drill_delta",
                ok,
                f"staging={staging}, production={production}",
            )
        )

    passed = all(item["passed"] for item in gates)
    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "status": "passed" if passed else "failed",
        "gates": gates,
    }

    json_path = out_dir / "release-readiness-checklist-report.json"
    md_path = out_dir / "release-readiness-checklist-report.md"
    json_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    lines = [
        "# Release Readiness Checklist Report",
        "",
        f"Generated at: {report['generatedAt']}",
        f"Status: `{report['status']}`",
        "",
        "| Gate | Status | Detail |",
        "|---|---|---|",
    ]
    for item in gates:
        lines.append(
            f"| {item['name']} | {'passed' if item['passed'] else 'failed'} | {item['detail']} |"
        )
    md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
