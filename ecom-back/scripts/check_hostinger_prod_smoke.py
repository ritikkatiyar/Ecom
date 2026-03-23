#!/usr/bin/env python3
"""Basic public-edge smoke checks for the Hostinger VPS stack."""

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request


def fetch(url: str, timeout: float) -> tuple[int, str]:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": "hostinger-prod-smoke/1.0",
            "X-API-Version": "v1",
        },
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read().decode("utf-8", errors="replace")
        return response.getcode(), body


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--base-url",
        default="http://127.0.0.1",
        help="Public base URL for the Hostinger deployment.",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=8.0,
        help="HTTP timeout per request.",
    )
    args = parser.parse_args()

    base_url = args.base_url.rstrip("/")
    checks = [
        ("nginx-health", "/healthz", 200, None),
        ("home-page", "/", 200, None),
        ("frontend-flags", "/api/internal/frontend-flags", 200, "application/json"),
        ("products", "/api/products", 200, "application/json"),
    ]

    failures: list[str] = []
    for name, path, expected_status, expected_content_type in checks:
        url = f"{base_url}{path}"
        try:
            status, body = fetch(url, args.timeout_seconds)
        except urllib.error.HTTPError as exc:
            failures.append(f"{name}: {url} returned HTTP {exc.code}")
            continue
        except Exception as exc:  # pragma: no cover - smoke script
            failures.append(f"{name}: {url} failed with {exc}")
            continue

        if status != expected_status:
            failures.append(
                f"{name}: {url} returned HTTP {status}, expected {expected_status}"
            )
            continue

        if expected_content_type == "application/json":
            try:
                json.loads(body)
            except json.JSONDecodeError:
                failures.append(f"{name}: {url} did not return valid JSON")
                continue

        print(f"[OK] {name}: {url}")

    if failures:
        print("")
        print("Hostinger smoke checks failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("")
    print("Hostinger smoke checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
