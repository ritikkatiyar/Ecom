#!/usr/bin/env python3
"""
Fetch observed production load telemetry from Prometheus and export OBSERVED_* env values.

Output:
- Writes an env file (KEY=VALUE lines) for GitHub Actions ingestion.
- Writes JSON/Markdown reports for traceability.
"""

from __future__ import annotations

import json
import os
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


def env(name: str, default: str = "") -> str:
    return (os.getenv(name) or default).strip()


def env_float(name: str, default: float) -> float:
    raw = env(name, str(default))
    try:
        return float(raw)
    except ValueError:
        return default


def default_queries() -> dict[str, str]:
    return {
        "OBSERVED_BROWSE_P95_MS": (
            '1000 * histogram_quantile(0.95, sum by (le) '
            '(rate(http_server_requests_seconds_bucket{job="product-service",method="GET",uri=~"/api/products.*"}[7d])))'
        ),
        "OBSERVED_BROWSE_FAIL_RATE": (
            '(sum(rate(http_server_requests_seconds_count{job="product-service",method="GET",uri=~"/api/products.*",status=~"5.."}[7d]))'
            ' / clamp_min(sum(rate(http_server_requests_seconds_count{job="product-service",method="GET",uri=~"/api/products.*"}[7d])), 1))'
        ),
        "OBSERVED_CART_P95_MS": (
            '1000 * histogram_quantile(0.95, sum by (le) '
            '(rate(http_server_requests_seconds_bucket{job="cart-service",uri=~"/api/cart.*"}[7d])))'
        ),
        "OBSERVED_CART_FAIL_RATE": (
            '(sum(rate(http_server_requests_seconds_count{job="cart-service",uri=~"/api/cart.*",status=~"5.."}[7d]))'
            ' / clamp_min(sum(rate(http_server_requests_seconds_count{job="cart-service",uri=~"/api/cart.*"}[7d])), 1))'
        ),
        "OBSERVED_CART_CONSISTENCY": (
            '(sum(rate(http_server_requests_seconds_count{job="cart-service",uri=~"/api/cart.*",status=~"2.."}[7d]))'
            ' / clamp_min(sum(rate(http_server_requests_seconds_count{job="cart-service",uri=~"/api/cart.*"}[7d])), 1))'
        ),
        "OBSERVED_CHECKOUT_P95_MS": (
            '1000 * histogram_quantile(0.95, sum by (le) '
            '(rate(http_server_requests_seconds_bucket{job="order-service",method="POST",uri="/api/orders"}[7d])))'
        ),
        "OBSERVED_CHECKOUT_FAIL_RATE": (
            '(sum(rate(http_server_requests_seconds_count{job="order-service",method="POST",uri="/api/orders",status=~"5.."}[7d]))'
            ' / clamp_min(sum(rate(http_server_requests_seconds_count{job="order-service",method="POST",uri="/api/orders"}[7d])), 1))'
        ),
        "OBSERVED_CHECKOUT_SUCCESS": (
            '(sum(rate(http_server_requests_seconds_count{job="order-service",method="POST",uri="/api/orders",status=~"2.."}[7d]))'
            ' / clamp_min(sum(rate(http_server_requests_seconds_count{job="order-service",method="POST",uri="/api/orders"}[7d])), 1))'
        ),
    }


def fallback_values() -> dict[str, float]:
    return {
        "OBSERVED_BROWSE_P95_MS": env_float("FALLBACK_OBSERVED_BROWSE_P95_MS", 230),
        "OBSERVED_BROWSE_FAIL_RATE": env_float("FALLBACK_OBSERVED_BROWSE_FAIL_RATE", 0.015),
        "OBSERVED_CART_P95_MS": env_float("FALLBACK_OBSERVED_CART_P95_MS", 260),
        "OBSERVED_CART_FAIL_RATE": env_float("FALLBACK_OBSERVED_CART_FAIL_RATE", 0.02),
        "OBSERVED_CART_CONSISTENCY": env_float("FALLBACK_OBSERVED_CART_CONSISTENCY", 0.991),
        "OBSERVED_CHECKOUT_P95_MS": env_float("FALLBACK_OBSERVED_CHECKOUT_P95_MS", 360),
        "OBSERVED_CHECKOUT_FAIL_RATE": env_float("FALLBACK_OBSERVED_CHECKOUT_FAIL_RATE", 0.03),
        "OBSERVED_CHECKOUT_SUCCESS": env_float("FALLBACK_OBSERVED_CHECKOUT_SUCCESS", 0.965),
    }


def fetch_query(prom_base_url: str, query: str, token: str) -> float | None:
    endpoint = prom_base_url.rstrip("/") + "/api/v1/query"
    params = urllib.parse.urlencode({"query": query})
    req = urllib.request.Request(endpoint + "?" + params, method="GET")
    req.add_header("Accept", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(req, timeout=20) as response:
        payload = json.loads(response.read().decode("utf-8"))

    if payload.get("status") != "success":
        return None
    result = payload.get("data", {}).get("result", [])
    if not result:
        return None
    value = result[0].get("value", [])
    if len(value) < 2:
        return None
    try:
        return float(value[1])
    except (TypeError, ValueError):
        return None


def normalize_metric(key: str, value: float) -> float:
    if key.endswith("_FAIL_RATE"):
        return max(0.0, min(value, 1.0))
    if key.endswith("_SUCCESS") or key.endswith("_CONSISTENCY"):
        return max(0.0, min(value, 1.0))
    return max(0.0, value)


def main() -> int:
    prom_url = env("PROMETHEUS_BASE_URL")
    prom_token = env("PROMETHEUS_BEARER_TOKEN")
    output_dir = Path(env("CALIBRATION_OUTPUT_DIR", "build-artifacts"))
    output_dir.mkdir(parents=True, exist_ok=True)
    env_output = Path(env("TELEMETRY_ENV_OUTPUT", str(output_dir / "load-budget-observed.env")))

    queries = default_queries()
    # Optional query overrides by env name: PROM_QUERY_<OBSERVED_KEY>
    for observed_key in list(queries.keys()):
        override = env(f"PROM_QUERY_{observed_key}")
        if override:
            queries[observed_key] = override

    fallback = fallback_values()
    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "prometheusBaseUrlConfigured": bool(prom_url),
        "metrics": [],
    }

    resolved: dict[str, float] = {}
    for key, query in queries.items():
        source = "fallback"
        value = fallback[key]
        error = ""
        if prom_url:
            try:
                fetched = fetch_query(prom_url, query, prom_token)
                if fetched is not None:
                    source = "prometheus"
                    value = fetched
                else:
                    error = "empty_result"
            except Exception as ex:  # noqa: BLE001
                error = f"{type(ex).__name__}: {ex}"
        value = normalize_metric(key, value)
        resolved[key] = value
        report["metrics"].append(
            {
                "name": key,
                "value": value,
                "source": source,
                "error": error,
                "query": query,
            }
        )

    env_lines = [f"{k}={v}" for k, v in resolved.items()]
    env_output.write_text("\n".join(env_lines) + "\n", encoding="utf-8")

    json_path = output_dir / "load-budget-telemetry-report.json"
    md_path = output_dir / "load-budget-telemetry-report.md"
    json_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    md_lines = [
        "# Load Budget Telemetry Ingestion Report",
        "",
        f"Generated at: {report['generatedAt']}",
        "",
        f"Prometheus configured: `{str(report['prometheusBaseUrlConfigured']).lower()}`",
        "",
        "| Metric | Value | Source | Error |",
        "|---|---:|---|---|",
    ]
    for metric in report["metrics"]:
        md_lines.append(
            f"| {metric['name']} | {metric['value']} | {metric['source']} | {metric['error'] or '-'} |"
        )
    md_path.write_text("\n".join(md_lines) + "\n", encoding="utf-8")

    print(f"Wrote {env_output}")
    print(f"Wrote {json_path}")
    print(f"Wrote {md_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
