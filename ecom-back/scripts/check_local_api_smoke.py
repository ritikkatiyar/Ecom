#!/usr/bin/env python3
"""
Local end-to-end API smoke suite.

Checks:
1. Direct service health endpoints.
2. Core gateway-facing endpoints.
3. Gateway API contracts (delegates to check_gateway_api_contracts.py).
"""

from __future__ import annotations

import json
import subprocess
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


@dataclass
class CheckResult:
    name: str
    passed: bool
    detail: str


def _http_check(
    name: str,
    url: str,
    expected: set[int],
    headers: dict[str, str] | None = None,
    timeout: int = 12,
    retries: int = 4,
    retry_delay_seconds: float = 3.0,
) -> CheckResult:
    last_detail = "no attempts"
    for attempt in range(1, retries + 1):
        request = urllib.request.Request(url=url, method="GET", headers=headers or {})
        try:
            with urllib.request.urlopen(request, timeout=timeout) as response:
                ok = response.status in expected
                detail = f"status={response.status}, url={url}, attempt={attempt}/{retries}"
                if ok:
                    return CheckResult(name=name, passed=True, detail=detail)
                last_detail = detail
        except urllib.error.HTTPError as ex:
            code = ex.code
            detail = f"status={code}, url={url}, attempt={attempt}/{retries}"
            if code in expected:
                return CheckResult(name=name, passed=True, detail=detail)
            last_detail = detail
        except Exception as ex:  # noqa: BLE001
            last_detail = f"{type(ex).__name__}: {ex}, url={url}, attempt={attempt}/{retries}"

        if attempt < retries:
            time.sleep(retry_delay_seconds)

    return CheckResult(name=name, passed=False, detail=last_detail)


def _run_gateway_contracts(repo_root: Path) -> CheckResult:
    script = repo_root / "ecom-back" / "scripts" / "check_gateway_api_contracts.py"
    report = repo_root / "build-artifacts" / "gateway-api-contracts-report.json"
    cmd = [sys.executable, str(script)]
    completed = subprocess.run(cmd, cwd=repo_root, capture_output=True, text=True, timeout=120)

    if not report.exists():
        return CheckResult(
            name="gateway.contracts",
            passed=False,
            detail=f"report not generated; exit={completed.returncode}",
        )

    data = json.loads(report.read_text(encoding="utf-8"))
    passed = data.get("failed", 0) == 0 and completed.returncode == 0
    detail = f"passed={data.get('passed')}, failed={data.get('failed')}, total={data.get('total')}, exit={completed.returncode}"
    return CheckResult(name="gateway.contracts", passed=passed, detail=detail)


def main() -> int:
    repo_root = Path(__file__).resolve().parents[2]
    out_dir = repo_root / "build-artifacts"
    out_dir.mkdir(parents=True, exist_ok=True)

    checks: list[CheckResult] = []

    service_health = [
        ("gateway.health", "http://localhost:8080/actuator/health"),
        ("auth.health", "http://localhost:8081/actuator/health"),
        ("user.health", "http://localhost:8082/actuator/health"),
        ("product.health", "http://localhost:8083/actuator/health"),
        ("inventory.health", "http://localhost:8084/actuator/health"),
        ("cart.health", "http://localhost:8085/actuator/health"),
        ("order.health", "http://localhost:8086/actuator/health"),
        ("payment.health", "http://localhost:8087/actuator/health"),
        ("review.health", "http://localhost:8088/actuator/health"),
        ("search.health", "http://localhost:8089/internal/health"),
        ("notification.health", "http://localhost:8090/actuator/health"),
    ]
    for name, url in service_health:
        checks.append(_http_check(name, url, {200}))

    checks.append(_http_check("storefront.root", "http://localhost:3000", {200, 307, 308}))
    checks.append(
        _http_check(
            "gateway.products.list",
            "http://localhost:8080/api/products?page=0&size=1",
            {200},
            headers={"X-API-Version": "v1"},
        )
    )
    checks.append(
        _http_check(
            "gateway.search.dataset.health",
            "http://localhost:8080/api/search/admin/relevance/dataset/health",
            {200},
            headers={"X-API-Version": "v1"},
        )
    )
    checks.append(_run_gateway_contracts(repo_root))

    passed = sum(1 for c in checks if c.passed)
    failed = len(checks) - passed
    status = "PASS" if failed == 0 else "FAIL"

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "status": status,
        "passed": passed,
        "failed": failed,
        "total": len(checks),
        "checks": [c.__dict__ for c in checks],
    }

    json_path = out_dir / "local-api-smoke-report.json"
    md_path = out_dir / "local-api-smoke-report.md"
    json_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    lines = [
        "# Local API Smoke Report",
        "",
        f"- Generated at: {report['generatedAt']}",
        f"- Status: `{status}`",
        f"- Passed: {passed}",
        f"- Failed: {failed}",
        "",
        "| Check | Result | Detail |",
        "|---|---|---|",
    ]
    for c in checks:
        lines.append(f"| {c.name} | {'PASS' if c.passed else 'FAIL'} | {c.detail} |")
    md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
