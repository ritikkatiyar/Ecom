#!/usr/bin/env python3
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path


def parse_iso8601(value: str) -> datetime:
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    return datetime.fromisoformat(value).astimezone(timezone.utc)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate search relevance dataset freshness.")
    parser.add_argument(
        "--metadata",
        default="ecom-back/services/search-service/src/main/resources/search-relevance-dataset-metadata.json",
        help="Path to dataset metadata JSON",
    )
    parser.add_argument(
        "--max-age-days",
        type=int,
        default=None,
        help="Override max staleness threshold (defaults to refreshCadenceDays in metadata)",
    )
    args = parser.parse_args()

    metadata_path = Path(args.metadata)
    if not metadata_path.exists():
        print(f"ERROR: metadata file not found: {metadata_path}")
        return 1

    data = json.loads(metadata_path.read_text(encoding="utf-8"))
    required = ["version", "lastRefreshedAt", "refreshCadenceDays"]
    missing = [k for k in required if k not in data]
    if missing:
        print(f"ERROR: metadata missing required fields: {missing}")
        return 1

    refreshed = parse_iso8601(data["lastRefreshedAt"])
    now = datetime.now(timezone.utc)
    age_days = max(0, (now - refreshed).days)
    max_age = args.max_age_days if args.max_age_days is not None else int(data["refreshCadenceDays"])

    print(
        f"Dataset version={data['version']} refreshed={data['lastRefreshedAt']} "
        f"age_days={age_days} max_age_days={max_age}"
    )

    if age_days > max_age:
        print("ERROR: search relevance dataset is stale.")
        return 1

    print("OK: search relevance dataset freshness check passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
