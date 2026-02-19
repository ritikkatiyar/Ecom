#!/usr/bin/env python3
"""
Validate Alertmanager receiver configuration for production readiness.

Checks:
- required keys are present
- values do not look like placeholders
- basic format checks for channels, emails, and URLs
"""

from __future__ import annotations

import json
import os
import re
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse


REQUIRED_KEYS = [
    "ALERTMANAGER_SLACK_API_URL",
    "ALERTMANAGER_AUDIT_WEBHOOK_URL",
    "ALERTMANAGER_SLACK_CHANNEL_CRITICAL",
    "ALERTMANAGER_SLACK_CHANNEL_ORDER",
    "ALERTMANAGER_SLACK_CHANNEL_NOTIFICATION",
    "ALERTMANAGER_SLACK_CHANNEL_PAYMENT",
    "ALERTMANAGER_PD_ROUTING_KEY_CRITICAL",
    "ALERTMANAGER_PD_ROUTING_KEY_ORDER",
    "ALERTMANAGER_PD_ROUTING_KEY_NOTIFICATION",
    "ALERTMANAGER_PD_ROUTING_KEY_PAYMENT",
    "ALERTMANAGER_EMAIL_ORDER",
    "ALERTMANAGER_EMAIL_NOTIFICATION",
    "ALERTMANAGER_EMAIL_PAYMENT",
]

PLACEHOLDER_TOKENS = [
    "replace-me",
    "example.com",
    "/REPLACE/ME",
    "changeme",
    "dummy",
]


def env(name: str) -> str:
    return (os.getenv(name) or "").strip()


def looks_placeholder(value: str) -> bool:
    normalized = value.lower()
    return any(token.lower() in normalized for token in PLACEHOLDER_TOKENS)


def valid_url(value: str) -> bool:
    parsed = urlparse(value)
    return parsed.scheme in {"http", "https"} and bool(parsed.netloc)


def valid_channel(value: str) -> bool:
    return bool(re.fullmatch(r"#[a-z0-9._-]+", value))


def valid_email(value: str) -> bool:
    return bool(re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", value))


def check_key(name: str, value: str) -> tuple[bool, str]:
    if not value:
        return False, "missing"
    if looks_placeholder(value):
        return False, "placeholder"

    if "_URL" in name:
        return (valid_url(value), "invalid_url" if not valid_url(value) else "ok")
    if "_CHANNEL_" in name:
        return (valid_channel(value), "invalid_channel" if not valid_channel(value) else "ok")
    if "_EMAIL_" in name:
        return (valid_email(value), "invalid_email" if not valid_email(value) else "ok")
    return True, "ok"


def main() -> int:
    out_dir = Path((os.getenv("CALIBRATION_OUTPUT_DIR") or "build-artifacts").strip())
    out_dir.mkdir(parents=True, exist_ok=True)

    results = []
    failures = []
    for key in REQUIRED_KEYS:
        value = env(key)
        ok, reason = check_key(key, value)
        results.append({"key": key, "ok": ok, "reason": reason})
        if not ok:
            failures.append(f"{key}:{reason}")

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "status": "passed" if not failures else "failed",
        "failures": failures,
        "results": results,
    }

    json_path = out_dir / "alertmanager-receiver-validation-report.json"
    md_path = out_dir / "alertmanager-receiver-validation-report.md"
    json_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    lines = [
        "# Alertmanager Receiver Validation Report",
        "",
        f"Generated at: {report['generatedAt']}",
        f"Status: `{report['status']}`",
        "",
        "| Key | Status | Reason |",
        "|---|---|---|",
    ]
    for result in results:
        lines.append(
            f"| {result['key']} | {'ok' if result['ok'] else 'failed'} | {result['reason']} |"
        )
    md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    if failures:
        print("Validation failures:")
        for item in failures:
            print(f"- {item}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
