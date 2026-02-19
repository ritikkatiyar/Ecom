#!/usr/bin/env python3
"""
Trigger environment rollback webhook after failed smoke checks.

Env vars:
  ROLLBACK_WEBHOOK_URL   Target webhook URL (required)
  ROLLBACK_ENVIRONMENT   staging|production (required)
  ROLLBACK_REASON        Human-readable reason (required)
  ROLLBACK_REF           Git ref (required)
  ROLLBACK_RUN_ID        GitHub run id (optional)
"""

from __future__ import annotations

import json
import os
import sys
import urllib.request


def _required(name: str) -> str:
    value = (os.getenv(name) or "").strip()
    if not value:
        print(f"{name} is required")
        sys.exit(2)
    return value


def main() -> int:
    webhook = _required("ROLLBACK_WEBHOOK_URL")
    payload = {
        "service": "ecom-backend",
        "environment": _required("ROLLBACK_ENVIRONMENT"),
        "reason": _required("ROLLBACK_REASON"),
        "ref": _required("ROLLBACK_REF"),
        "runId": (os.getenv("ROLLBACK_RUN_ID") or "").strip(),
    }

    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url=webhook,
        data=data,
        method="POST",
        headers={"Content-Type": "application/json", "User-Agent": "ecom-rollback-trigger/1.0"},
    )

    with urllib.request.urlopen(request, timeout=10) as response:
        print(f"Rollback webhook response status: {response.status}")
        if response.status < 200 or response.status >= 300:
            return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
