#!/usr/bin/env python3
from pathlib import Path


REQUIRED_SECTIONS = [
    "Endpoints",
    "Entities",
    "Data Stores",
    "Flow",
]

DOC_PATHS = [
    "ecom-back/api-gateway/API_DOCS.md",
    "ecom-back/services/auth-service/API_DOCS.md",
    "ecom-back/services/user-service/API_DOCS.md",
    "ecom-back/services/product-service/API_DOCS.md",
    "ecom-back/services/inventory-service/API_DOCS.md",
    "ecom-back/services/cart-service/API_DOCS.md",
    "ecom-back/services/order-service/API_DOCS.md",
    "ecom-back/services/payment-service/API_DOCS.md",
    "ecom-back/services/review-service/API_DOCS.md",
    "ecom-back/services/search-service/API_DOCS.md",
    "ecom-back/services/notification-service/API_DOCS.md",
]


def main() -> int:
    failed = []
    for path_str in DOC_PATHS:
        path = Path(path_str)
        if not path.exists():
            failed.append(f"{path}: missing file")
            continue

        text = path.read_text(encoding="utf-8")
        missing = [section for section in REQUIRED_SECTIONS if section not in text]
        if missing:
            failed.append(f"{path}: missing sections {missing}")

    if failed:
        print("API docs validation failed:")
        for line in failed:
            print(f"- {line}")
        return 1

    print("API docs validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
