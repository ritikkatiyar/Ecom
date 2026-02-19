#!/usr/bin/env python3
"""
Post-deploy smoke checks for staging/production release gates.

Env vars:
  SMOKE_URLS                Comma-separated URLs to probe (required)
  SMOKE_TIMEOUT_SECONDS     Per-request timeout (default: 5)
  SMOKE_BEARER_TOKEN        Optional bearer token for Authorization header
  SMOKE_EXPECTED_STATUS     Expected status code (default: 200)
"""

from __future__ import annotations

import os
import sys
import urllib.error
import urllib.request


def _read_env() -> tuple[list[str], int, str | None, int]:
    raw_urls = (os.getenv("SMOKE_URLS") or "").strip()
    if not raw_urls:
        print("SMOKE_URLS is required")
        sys.exit(2)
    urls = [u.strip() for u in raw_urls.split(",") if u.strip()]
    if not urls:
        print("SMOKE_URLS parsed to empty URL set")
        sys.exit(2)

    timeout = int(os.getenv("SMOKE_TIMEOUT_SECONDS", "5"))
    expected_status = int(os.getenv("SMOKE_EXPECTED_STATUS", "200"))
    token = (os.getenv("SMOKE_BEARER_TOKEN") or "").strip() or None
    return urls, timeout, token, expected_status


def _check_url(url: str, timeout: int, token: str | None, expected_status: int) -> bool:
    headers = {"User-Agent": "ecom-backend-smoke-check/1.0"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url=url, headers=headers, method="GET")

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            ok = response.status == expected_status
            print(f"[{'OK' if ok else 'FAIL'}] {url} -> {response.status}")
            return ok
    except urllib.error.HTTPError as ex:
        ok = ex.code == expected_status
        print(f"[{'OK' if ok else 'FAIL'}] {url} -> {ex.code} (HTTPError)")
        return ok
    except Exception as ex:  # noqa: BLE001
        print(f"[FAIL] {url} -> {type(ex).__name__}: {ex}")
        return False


def main() -> int:
    urls, timeout, token, expected_status = _read_env()
    failures = 0
    for url in urls:
        if not _check_url(url, timeout, token, expected_status):
            failures += 1
    if failures:
        print(f"Smoke check failed: {failures}/{len(urls)} probes failed")
        return 1
    print(f"Smoke check passed: {len(urls)}/{len(urls)} probes healthy")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
