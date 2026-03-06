#!/usr/bin/env python3
"""Validate gateway API contracts against live gateway responses."""

from __future__ import annotations

import argparse
import json
import os
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[2]
CONTRACTS_FILE = REPO_ROOT / "ecom-back" / "contracts" / "gateway" / "gateway-api-contracts.json"
ARTIFACTS_DIR = REPO_ROOT / "build-artifacts"
REPORT_JSON = ARTIFACTS_DIR / "gateway-api-contracts-report.json"
REPORT_MD = ARTIFACTS_DIR / "gateway-api-contracts-report.md"


def _parse_csv(value: str | None) -> set[str]:
    if not value:
        return set()
    return {item.strip() for item in value.split(",") if item.strip()}


def _matches_tags(contract_tags: set[str], include_tags: set[str], exclude_tags: set[str]) -> bool:
    if include_tags and contract_tags.isdisjoint(include_tags):
        return False
    if exclude_tags and contract_tags.intersection(exclude_tags):
        return False
    return True


def _load_contracts() -> list[dict[str, Any]]:
    payload = json.loads(CONTRACTS_FILE.read_text(encoding="utf-8"))
    contracts = payload.get("contracts")
    if not isinstance(contracts, list) or not contracts:
        raise ValueError("contracts file must contain a non-empty 'contracts' list")
    return contracts


def _normalize_headers(headers: dict[str, str]) -> dict[str, str]:
    return {key.lower(): value for key, value in headers.items()}


def _request_contract(
    contract: dict[str, Any],
    base_url: str,
    api_version: str,
    timeout_seconds: float,
) -> tuple[int, dict[str, str], str]:
    method = str(contract.get("method", "GET")).upper()
    path = str(contract.get("path", "")).strip()
    if not path.startswith("/"):
        raise ValueError(f"contract path must start with '/': {path}")

    include_api_version = bool(contract.get("includeApiVersion", True))
    headers = {"Accept": "application/json"}
    if include_api_version:
        headers["X-API-Version"] = api_version

    url = urllib.parse.urljoin(base_url.rstrip("/") + "/", path.lstrip("/"))
    req = urllib.request.Request(url=url, method=method, headers=headers)

    try:
        with urllib.request.urlopen(req, timeout=timeout_seconds) as response:
            body = response.read().decode("utf-8", errors="replace")
            response_headers = dict(response.getheaders())
            return response.getcode(), response_headers, body
    except urllib.error.HTTPError as http_error:
        body = http_error.read().decode("utf-8", errors="replace")
        response_headers = dict(http_error.headers.items()) if http_error.headers else {}
        return http_error.code, response_headers, body


def _validate_response(contract: dict[str, Any], status: int, headers: dict[str, str], body: str) -> list[str]:
    failures: list[str] = []
    expected_statuses = contract.get("expectedStatuses", [])
    if status not in expected_statuses:
        failures.append(f"status {status} not in expected {expected_statuses}")

    parsed: dict[str, Any] | None = None
    expect_json = bool(contract.get("expectJson", False))
    if expect_json:
        try:
            parsed = json.loads(body) if body else {}
        except json.JSONDecodeError as ex:
            failures.append(f"expected JSON response but parse failed: {ex}")

    expect_code = contract.get("expectCode")
    if expect_code and isinstance(parsed, dict):
        if parsed.get("code") != expect_code:
            failures.append(f"expected code '{expect_code}' but got '{parsed.get('code')}'")

    expect_fields = contract.get("expectBodyFields", [])
    if expect_fields and isinstance(parsed, dict):
        missing_fields = [field for field in expect_fields if field not in parsed]
        if missing_fields:
            failures.append("missing response body fields: " + ", ".join(missing_fields))

    required_headers = [str(h).lower() for h in contract.get("requireResponseHeaders", [])]
    normalized_headers = _normalize_headers(headers)
    missing_headers = [h for h in required_headers if h not in normalized_headers]
    if missing_headers:
        failures.append("missing response headers: " + ", ".join(missing_headers))

    return failures


def _write_report(report: dict[str, Any]) -> None:
    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, indent=2), encoding="utf-8")

    lines = [
        "# Gateway API Contracts Report",
        "",
        f"- Generated at: {report['generatedAt']}",
        f"- Base URL: `{report['baseUrl']}`",
        f"- Selected contracts: {report['total']}",
        f"- Passed: {report['passed']}",
        f"- Failed: {report['failed']}",
        f"- Skipped: {report['skipped']}",
        f"- Status: `{'PASS' if report['failed'] == 0 else 'FAIL'}`",
        "",
        "| Contract | Result | Detail |",
        "|---|---|---|",
    ]

    for result in report["results"]:
        detail = result.get("detail", "").replace("\n", " ").replace("|", "\\|")
        lines.append(f"| {result['name']} | {result['result']} | {detail} |")

    REPORT_MD.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="Check gateway API contracts against a live gateway URL.")
    parser.add_argument(
        "--base-url",
        default=os.getenv("GATEWAY_CONTRACT_BASE_URL", "http://localhost:8080"),
        help="Gateway base URL (default: env GATEWAY_CONTRACT_BASE_URL or http://localhost:8080)",
    )
    parser.add_argument(
        "--api-version",
        default=os.getenv("GATEWAY_API_VERSION", "v1"),
        help="Value to send for X-API-Version when includeApiVersion=true (default: v1)",
    )
    parser.add_argument(
        "--tags",
        default=os.getenv("GATEWAY_CONTRACT_TAGS", ""),
        help="Comma-separated include tags; if set, only contracts matching at least one tag run.",
    )
    parser.add_argument(
        "--exclude-tags",
        default=os.getenv("GATEWAY_CONTRACT_EXCLUDE_TAGS", ""),
        help="Comma-separated tags to exclude.",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=float(os.getenv("GATEWAY_CONTRACT_TIMEOUT_SECONDS", "8")),
        help="Per-request timeout in seconds.",
    )
    parser.add_argument(
        "--allow-unavailable",
        action="store_true",
        help="If set, network/unavailable errors are marked as skipped instead of failed.",
    )
    args = parser.parse_args()

    include_tags = _parse_csv(args.tags)
    exclude_tags = _parse_csv(args.exclude_tags)

    contracts = _load_contracts()
    selected = [
        contract
        for contract in contracts
        if _matches_tags(set(contract.get("tags", [])), include_tags, exclude_tags)
    ]
    if not selected:
        raise SystemExit("No contracts selected after applying tags/exclude-tags.")

    results: list[dict[str, Any]] = []
    passed = 0
    failed = 0
    skipped = 0

    for contract in selected:
        name = str(contract.get("name", "unnamed-contract"))
        try:
            status, headers, body = _request_contract(
                contract=contract,
                base_url=args.base_url,
                api_version=args.api_version,
                timeout_seconds=args.timeout_seconds,
            )
        except (urllib.error.URLError, TimeoutError, OSError) as ex:
            if args.allow_unavailable:
                skipped += 1
                results.append(
                    {
                        "name": name,
                        "result": "SKIPPED",
                        "detail": f"gateway unavailable: {ex}",
                    }
                )
                continue

            failed += 1
            results.append(
                {
                    "name": name,
                    "result": "FAIL",
                    "detail": f"gateway unavailable: {ex}",
                }
            )
            continue

        validation_failures = _validate_response(contract, status, headers, body)
        if validation_failures:
            failed += 1
            results.append(
                {
                    "name": name,
                    "result": "FAIL",
                    "detail": "; ".join(validation_failures),
                    "status": status,
                }
            )
        else:
            passed += 1
            results.append(
                {
                    "name": name,
                    "result": "PASS",
                    "detail": f"status={status}",
                    "status": status,
                }
            )

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "baseUrl": args.base_url,
        "tags": sorted(include_tags),
        "excludeTags": sorted(exclude_tags),
        "total": len(selected),
        "passed": passed,
        "failed": failed,
        "skipped": skipped,
        "results": results,
    }
    _write_report(report)

    print(f"Wrote {REPORT_JSON}")
    print(f"Wrote {REPORT_MD}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
