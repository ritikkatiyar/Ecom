#!/usr/bin/env python3
"""
Run release-gate rollback drill for staging/production and emit release-gate artifacts.

This script intentionally exercises rollback trigger + callback paths so that
`evaluate_release_gate_drills.py` can produce actionable threshold deltas.
"""

from __future__ import annotations

import json
import os
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def env(name: str, default: str = "") -> str:
    return (os.getenv(name) or default).strip()


def post_json(url: str, payload: dict, user_agent: str) -> tuple[bool, str]:
    if not url:
        return False, "missing_url"
    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url=url,
        data=data,
        method="POST",
        headers={"Content-Type": "application/json", "User-Agent": user_agent},
    )
    try:
        with urllib.request.urlopen(request, timeout=15) as response:
            if 200 <= response.status < 300:
                return True, f"http_{response.status}"
            return False, f"http_{response.status}"
    except Exception as ex:  # noqa: BLE001
        return False, f"{type(ex).__name__}: {ex}"


def run_env_drill(environment: str, ref: str, run_id: str) -> dict:
    prefix = environment.upper()
    rollback_webhook = env(f"{prefix}_ROLLBACK_WEBHOOK")
    callback_webhook = env(f"{prefix}_ROLLBACK_VERIFY_WEBHOOK")
    callback_metrics_url = env(f"{prefix}_RELEASE_GATE_METRICS_URL")

    rollback_payload = {
        "service": "ecom-backend",
        "environment": environment,
        "reason": "scheduled_release_gate_drill",
        "ref": ref,
        "runId": run_id,
    }
    trigger_ok, trigger_detail = post_json(
        rollback_webhook,
        rollback_payload,
        "ecom-release-gate-drill/1.0",
    )

    callback_payload = {
        "service": "ecom-backend",
        "event": "rollback_verification",
        "environment": environment,
        "status": "triggered",
        "ref": ref,
        "runId": run_id,
        "details": "scheduled_release_gate_drill",
    }
    callback_ok, callback_detail = post_json(
        callback_webhook,
        callback_payload,
        "ecom-release-gate-drill/1.0",
    )

    metrics_ok = True
    metrics_detail = "skipped"
    if callback_metrics_url:
        metrics_ok, metrics_detail = post_json(
            callback_metrics_url,
            callback_payload,
            "ecom-release-gate-drill/1.0",
        )

    return {
        "environment": environment,
        "smokeOutcome": "failure",
        "rollbackTriggerOutcome": "success" if trigger_ok else "failure",
        "rollbackTriggerDetail": trigger_detail,
        "rollbackCallbackOutcome": "success" if (callback_ok and metrics_ok) else "failure",
        "rollbackCallbackDetail": callback_detail,
        "rollbackMetricsDetail": metrics_detail,
        "runId": run_id,
        "ref": ref,
        "drillType": "scheduled_release_gate_drill",
    }


def main() -> int:
    out_dir = Path(env("DRILL_OUTPUT_DIR", "build-artifacts"))
    out_dir.mkdir(parents=True, exist_ok=True)

    ref = env("DRILL_REF", env("GITHUB_REF", "refs/heads/main"))
    run_id = env("DRILL_RUN_ID", env("GITHUB_RUN_ID", "local"))
    environments = [item.strip().lower() for item in env("DRILL_ENVIRONMENTS", "staging,production").split(",") if item.strip()]

    results: list[dict] = []
    for environment in environments:
        if environment not in {"staging", "production"}:
            continue
        result = run_env_drill(environment, ref, run_id)
        results.append(result)
        (out_dir / f"{environment}-release-gate.json").write_text(
            json.dumps(result, indent=2),
            encoding="utf-8",
        )

    overall = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "results": results,
    }
    (out_dir / "release-gate-drill-report.json").write_text(json.dumps(overall, indent=2), encoding="utf-8")

    failed = [r for r in results if r["rollbackTriggerOutcome"] != "success" or r["rollbackCallbackOutcome"] != "success"]
    print(f"Wrote {out_dir / 'release-gate-drill-report.json'}")
    return 0 if not failed else 1


if __name__ == "__main__":
    raise SystemExit(main())
