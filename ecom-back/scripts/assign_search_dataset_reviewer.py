#!/usr/bin/env python3
"""
Assign search relevance dataset reviewer by rotation and cadence.

Outputs JSON with:
  reviewer, due, stale, ageDays, cadenceDays, lastRefreshedAt, nextReviewDueAt
Optionally writes a markdown report.
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path


def parse_iso8601(value: str) -> datetime:
    if value.endswith("Z"):
        value = value[:-1] + "+00:00"
    return datetime.fromisoformat(value).astimezone(timezone.utc)


def iso_day_start(ts: datetime) -> datetime:
    return datetime(ts.year, ts.month, ts.day, tzinfo=timezone.utc)


def choose_reviewer(reviewers: list[str], now: datetime) -> str:
    if not reviewers:
        return ""
    iso_week = now.isocalendar().week
    return reviewers[(iso_week - 1) % len(reviewers)]


def parse_reviewers_csv(raw: str) -> list[str]:
    reviewers: list[str] = []
    seen: set[str] = set()
    for item in raw.split(","):
        candidate = item.strip()
        if not candidate:
            continue
        normalized = candidate.lower()
        if normalized in seen:
            continue
        seen.add(normalized)
        reviewers.append(candidate)
    return reviewers


def main() -> int:
    parser = argparse.ArgumentParser(description="Assign search dataset reviewer by rotation.")
    parser.add_argument(
        "--metadata",
        default="ecom-back/services/search-service/src/main/resources/search-relevance-dataset-metadata.json",
        help="Path to dataset metadata JSON",
    )
    parser.add_argument(
        "--ownership",
        default="ecom-back/services/search-service/src/main/resources/search-relevance-dataset-ownership.json",
        help="Path to ownership rotation JSON",
    )
    parser.add_argument(
        "--report-out",
        default="",
        help="Optional markdown report output path",
    )
    parser.add_argument(
        "--reviewers-csv",
        default="",
        help="Optional comma-separated reviewer override (used for onboarding without changing JSON).",
    )
    parser.add_argument(
        "--min-reviewers",
        type=int,
        default=1,
        help="Minimum required reviewer count for healthy rotation pool.",
    )
    args = parser.parse_args()

    metadata_path = Path(args.metadata)
    ownership_path = Path(args.ownership)
    if not metadata_path.exists():
        raise SystemExit(f"ERROR: metadata file not found: {metadata_path}")
    if not ownership_path.exists():
        raise SystemExit(f"ERROR: ownership file not found: {ownership_path}")

    metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
    ownership = json.loads(ownership_path.read_text(encoding="utf-8"))

    refreshed = parse_iso8601(metadata["lastRefreshedAt"])
    cadence_days = int(metadata["refreshCadenceDays"])
    now = datetime.now(timezone.utc)
    age_days = max(0, (iso_day_start(now) - iso_day_start(refreshed)).days)
    due = age_days >= cadence_days
    stale = age_days > cadence_days
    next_due_at = iso_day_start(refreshed) + timedelta(days=cadence_days)

    reviewers = ownership.get("reviewers", [])
    reviewers = parse_reviewers_csv(",".join(reviewers))
    if args.reviewers_csv.strip():
        reviewers = parse_reviewers_csv(args.reviewers_csv)
    reviewer = choose_reviewer(reviewers, now)
    reviewer_pool_healthy = len(reviewers) >= max(1, args.min_reviewers)
    issue_cfg = ownership.get("issue", {})
    auto_close = bool(issue_cfg.get("autoCloseWhenNotDue", True))

    result = {
        "reviewer": reviewer,
        "reviewerPool": reviewers,
        "reviewerCount": len(reviewers),
        "reviewerPoolHealthy": reviewer_pool_healthy,
        "minReviewersRequired": max(1, args.min_reviewers),
        "due": due,
        "stale": stale,
        "ageDays": age_days,
        "cadenceDays": cadence_days,
        "lastRefreshedAt": metadata["lastRefreshedAt"],
        "nextReviewDueAt": next_due_at.isoformat().replace("+00:00", "Z"),
        "datasetVersion": metadata.get("version", "unknown"),
        "issueTitle": issue_cfg.get("title", "[Search Dataset] Refresh review due"),
        "issueLabels": issue_cfg.get("labels", ["search-dataset-review", "search-service", "ops"]),
        "autoCloseWhenNotDue": auto_close,
    }

    if args.report_out:
        report_path = Path(args.report_out)
        report_path.parent.mkdir(parents=True, exist_ok=True)
        report_path.write_text(
            "\n".join(
                [
                    "# Search Dataset Rotation Report",
                    "",
                    f"- Dataset version: `{result['datasetVersion']}`",
                    f"- Last refreshed at: `{result['lastRefreshedAt']}`",
                    f"- Cadence days: `{result['cadenceDays']}`",
                    f"- Age days: `{result['ageDays']}`",
                    f"- Due: `{str(result['due']).lower()}`",
                    f"- Stale: `{str(result['stale']).lower()}`",
                    f"- Next due at: `{result['nextReviewDueAt']}`",
                    f"- Assigned reviewer: `{result['reviewer'] or 'unassigned'}`",
                    f"- Reviewer pool size: `{result['reviewerCount']}`",
                    f"- Reviewer pool healthy: `{str(result['reviewerPoolHealthy']).lower()}`",
                    f"- Min reviewers required: `{result['minReviewersRequired']}`",
                    f"- Auto-close when not due: `{str(result['autoCloseWhenNotDue']).lower()}`",
                ]
            ),
            encoding="utf-8",
        )

    print(json.dumps(result))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
