#!/usr/bin/env python3
"""Validate Kafka event contract registry against code usage and schema files."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
CONTRACT_FILE = REPO_ROOT / "ecom-back" / "contracts" / "events" / "event-contracts.json"
ARTIFACTS_DIR = REPO_ROOT / "build-artifacts"
REPORT_JSON = ARTIFACTS_DIR / "event-contracts-report.json"
REPORT_MD = ARTIFACTS_DIR / "event-contracts-report.md"

EVENT_TYPE_PATTERN = re.compile(r"\b[a-z]+(?:\.[a-z-]+)+\.v\d+\b")
EVENT_NAME_RULE = re.compile(r"^[a-z]+(?:\.[a-z-]+)+\.v(\d+)$")
ALLOWED_EVENT_ROOTS = {"order", "payment", "inventory", "product", "notification"}

SCAN_DIRS = [
    REPO_ROOT / "ecom-back" / "services",
    REPO_ROOT / "ecom-back" / "common",
]
SCAN_SUFFIXES = {".java", ".yml", ".yaml", ".json"}


@dataclass
class ValidationResult:
    passed: bool
    errors: list[str]
    warnings: list[str]
    discovered_events: list[str]
    registered_events: list[str]


def _load_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def _discover_events() -> set[str]:
    found: set[str] = set()
    for base in SCAN_DIRS:
        if not base.exists():
            continue
        for file_path in base.rglob("*"):
            if file_path.suffix.lower() not in SCAN_SUFFIXES:
                continue
            try:
                content = file_path.read_text(encoding="utf-8")
            except UnicodeDecodeError:
                continue
            for event_type in EVENT_TYPE_PATTERN.findall(content):
                root = event_type.split(".", 1)[0]
                if root in ALLOWED_EVENT_ROOTS:
                    found.add(event_type)
    return found


def _validate_contracts(contracts: dict, discovered: set[str]) -> ValidationResult:
    errors: list[str] = []
    warnings: list[str] = []

    events = contracts.get("events")
    if not isinstance(events, dict) or not events:
        errors.append("Contract registry must define a non-empty 'events' object.")
        return ValidationResult(False, errors, warnings, sorted(discovered), [])

    registered_events = set(events.keys())

    for event_type, metadata in events.items():
        if not EVENT_NAME_RULE.match(event_type):
            errors.append(f"Invalid event name format: {event_type}")
            continue

        if not isinstance(metadata, dict):
            errors.append(f"Metadata for {event_type} must be an object.")
            continue

        schema_version = metadata.get("schema_version")
        producer = metadata.get("producer")
        consumers = metadata.get("consumers")
        schema_file = metadata.get("schema_file")

        version_match = EVENT_NAME_RULE.match(event_type)
        expected_schema_version = f"v{version_match.group(1)}" if version_match else None
        if schema_version != expected_schema_version:
            errors.append(
                f"{event_type}: schema_version '{schema_version}' does not match event version '{expected_schema_version}'."
            )

        if not isinstance(producer, str) or not producer.strip():
            errors.append(f"{event_type}: producer must be a non-empty string.")

        if not isinstance(consumers, list):
            errors.append(f"{event_type}: consumers must be a list.")

        if not isinstance(schema_file, str) or not schema_file.strip():
            errors.append(f"{event_type}: schema_file must be a non-empty string.")
            continue

        resolved_schema = CONTRACT_FILE.parent / schema_file
        if not resolved_schema.exists():
            errors.append(f"{event_type}: schema file not found at {resolved_schema}.")
            continue

        try:
            schema = _load_json(resolved_schema)
        except json.JSONDecodeError as ex:
            errors.append(f"{event_type}: invalid schema JSON ({ex}).")
            continue

        if schema.get("type") != "object":
            errors.append(f"{event_type}: schema type must be 'object'.")
        if "$schema" not in schema:
            warnings.append(f"{event_type}: schema does not declare '$schema'.")

    unknown_events = sorted(discovered - registered_events)
    if unknown_events:
        errors.append(
            "Discovered event types missing from registry: " + ", ".join(unknown_events)
        )

    unused_events = sorted(registered_events - discovered)
    for event_type in unused_events:
        warnings.append(f"Registered event not currently discovered in code scan: {event_type}")

    passed = not errors
    return ValidationResult(
        passed=passed,
        errors=errors,
        warnings=warnings,
        discovered_events=sorted(discovered),
        registered_events=sorted(registered_events),
    )


def _write_reports(result: ValidationResult) -> None:
    ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)

    report = {
        "passed": result.passed,
        "errors": result.errors,
        "warnings": result.warnings,
        "discoveredEvents": result.discovered_events,
        "registeredEvents": result.registered_events,
    }
    REPORT_JSON.write_text(json.dumps(report, indent=2), encoding="utf-8")

    lines = [
        "# Event Contracts Report",
        "",
        f"- Status: {'PASS' if result.passed else 'FAIL'}",
        f"- Registered events: {len(result.registered_events)}",
        f"- Discovered events: {len(result.discovered_events)}",
        "",
    ]

    if result.errors:
        lines.extend(["## Errors", ""])
        lines.extend(f"- {error}" for error in result.errors)
        lines.append("")

    if result.warnings:
        lines.extend(["## Warnings", ""])
        lines.extend(f"- {warning}" for warning in result.warnings)
        lines.append("")

    REPORT_MD.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    contracts = _load_json(CONTRACT_FILE)
    discovered = _discover_events()
    result = _validate_contracts(contracts, discovered)
    _write_reports(result)
    return 0 if result.passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
