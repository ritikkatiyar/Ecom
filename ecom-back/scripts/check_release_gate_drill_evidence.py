#!/usr/bin/env python3
"""Validate release-gate drill delta evidence status for staging and production."""

from __future__ import annotations

import json
from pathlib import Path

REPORT_PATH = Path("build-artifacts/release-gate-drill-delta-report.json")


def read_report(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(f"Delta report not found: {path}")
    return json.loads(path.read_text(encoding="utf-8"))


def status_of(report: dict, environment: str) -> str:
    section = report.get(environment) or {}
    return str(section.get("status", "missing")).strip().lower()


def main() -> int:
    try:
        report = read_report(REPORT_PATH)
    except FileNotFoundError as exc:
        print(str(exc))
        return 1

    staging = status_of(report, "staging")
    production = status_of(report, "production")
    blocked = {"missing", "attention_required"}

    print(f"staging status: {staging}")
    print(f"production status: {production}")

    if staging in blocked or production in blocked:
        print("Release-gate drill evidence is not healthy yet; keep tuning and rerun drills.")
        return 1

    print("Release-gate drill evidence is healthy for both staging and production.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
