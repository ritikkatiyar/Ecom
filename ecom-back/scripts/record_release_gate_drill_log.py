#!/usr/bin/env python3
"""
Record a release-gate drill result row from delta report artifacts.
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path


def load_delta(path: Path) -> dict:
    if not path.exists():
        raise SystemExit(f"Delta report not found: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def decide_delta_applied(delta_items: list[dict]) -> str:
    if not delta_items:
        return "no"
    # Conservative default for automated log: manual review required before applying threshold changes.
    return "no"


def decide_note(delta: dict) -> str:
    deltas = delta.get("recommendedDeltas", [])
    if not deltas:
        return "no_delta_recommendation"
    first = deltas[0]
    reason = first.get("reason", "n/a")
    setting = first.get("setting", "n/a")
    return f"setting={setting}; reason={reason}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Record release gate drill row from delta artifact.")
    parser.add_argument("--delta-report", default="build-artifacts/release-gate-drill-delta-report.json")
    parser.add_argument("--owner", default="platform-oncall")
    parser.add_argument("--out", default="build-artifacts/release-gate-drill-log.md")
    args = parser.parse_args()

    delta = load_delta(Path(args.delta_report))
    date_utc = datetime.now(timezone.utc).date().isoformat()
    staging_status = (delta.get("staging") or {}).get("status", "missing")
    production_status = (delta.get("production") or {}).get("status", "missing")
    delta_applied = decide_delta_applied(delta.get("recommendedDeltas", []))
    note = decide_note(delta)

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    header = [
        "# Release Gate Drill Log",
        "",
        "| Date (UTC) | Staging Status | Production Status | Delta Applied | Owner | Notes |",
        "|---|---|---|---|---|---|",
    ]
    row = f"| {date_utc} | {staging_status} | {production_status} | {delta_applied} | {args.owner} | {note} |"

    if not out_path.exists():
        out_path.write_text("\n".join(header + [row]) + "\n", encoding="utf-8")
    else:
        existing = out_path.read_text(encoding="utf-8").rstrip()
        out_path.write_text(existing + "\n" + row + "\n", encoding="utf-8")

    print(f"Wrote {out_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
