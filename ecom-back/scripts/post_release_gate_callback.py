#!/usr/bin/env python3
"""
Post release-gate callback payload to an external verification endpoint.

Required env vars:
  CALLBACK_WEBHOOK_URL
  CALLBACK_EVENT
  CALLBACK_ENVIRONMENT
  CALLBACK_STATUS
  CALLBACK_REF
  CALLBACK_RUN_ID

Optional env vars:
  CALLBACK_DETAILS
  CALLBACK_METRICS_URL
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
    webhook = _required("CALLBACK_WEBHOOK_URL")
    payload = {
        "service": "ecom-backend",
        "event": _required("CALLBACK_EVENT"),
        "environment": _required("CALLBACK_ENVIRONMENT"),
        "status": _required("CALLBACK_STATUS"),
        "ref": _required("CALLBACK_REF"),
        "runId": _required("CALLBACK_RUN_ID"),
        "details": (os.getenv("CALLBACK_DETAILS") or "").strip(),
    }

    data = json.dumps(payload).encode("utf-8")
    request = urllib.request.Request(
        url=webhook,
        data=data,
        method="POST",
        headers={"Content-Type": "application/json", "User-Agent": "ecom-release-gate-callback/1.0"},
    )

    with urllib.request.urlopen(request, timeout=10) as response:
        print(f"Release-gate callback response status: {response.status}")
        if response.status < 200 or response.status >= 300:
            return 1

    metrics_url = (os.getenv("CALLBACK_METRICS_URL") or "").strip()
    if metrics_url:
        metrics_request = urllib.request.Request(
            url=metrics_url,
            data=data,
            method="POST",
            headers={"Content-Type": "application/json", "User-Agent": "ecom-release-gate-callback/1.0"},
        )
        with urllib.request.urlopen(metrics_request, timeout=10) as metrics_response:
            print(f"Release-gate metrics response status: {metrics_response.status}")
            if metrics_response.status < 200 or metrics_response.status >= 300:
                return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
