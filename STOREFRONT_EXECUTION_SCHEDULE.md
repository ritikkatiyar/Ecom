# Storefront Completion Schedule (Stitch-Aligned)

Last updated: 2026-02-26

## Scope Lock
- Active frontend: `ecom-storefront` only.
- Design source of truth: `stitch/` folder (`voluspa_product_page_*`, `admin_dashboard`, `user_account_page`).
- Goal: ship a fully backend-mapped storefront with stitch visual language, then harden.

## Phase A (Now) - Product Discovery Mapping
Status: `Done`

1. Product listing pages mapped to backend:
- `/` (featured products)
- `/shop` (catalog grid)
2. Product detail mapped to backend:
- `/products/[id]`
3. APIs used:
- `GET /api/products`
- `GET /api/products/{id}`
- Proxied through storefront routes under `app/api/products/**`

Acceptance:
- Product created from admin appears on `/shop` and `/`.
- Clicking a product opens real detail page.

## Phase B - Search + Collections
Status: `Done`

1. Wire `/search` to backend search API. `Done`
2. Add debounced query UX and empty/error states. `Done`
3. Add collection sections (category/brand-based) with stitch cards. `Done`

APIs:
- `GET /api/search/products`
- `GET /api/products` with query filters

Acceptance:
- Search returns live products with debounce and pagination controls.

## Phase C - Cart + Checkout Baseline
Status: `In Progress`

1. Wire add-to-cart from product list/detail. `Done` (detail mapped)
2. Wire cart page read/update/remove. `Done`
3. Guest + logged-in path handling aligned with backend cart model. `Done` (guest primary, numeric user path supported)
4. Checkout saga status polling (`PAYMENT_PENDING`, `CONFIRMED`, `FAILED`, `TIMED_OUT`). `Done`
5. Redirect to login when checkout requires authenticated numeric user id. `Done`

APIs:
- `GET /api/cart`
- `POST /api/cart/items`
- `DELETE /api/cart/items/{productId}`
- `DELETE /api/cart`
- `POST /api/orders`
- `GET /api/orders/{orderId}`
- `POST /api/payments/intents`

Acceptance:
- Cart count and cart page stay in sync after add/remove/clear.
- Checkout creates order + payment intent and renders live status transitions.

## Phase D - Auth + Account
Status: `Done`

1. Login/signup/logout UX finalized. `Done`
2. Protect account/admin routes via guards. `Done`
3. Map account page sections to user/order endpoints. `Done` (dashboard cards + recent orders)
4. Expand account route group (`orders`, `profile`, `addresses`, `preferences`, `security`, `wishlist`, `notifications`). `Done`

Completed:
- Access token kept in memory only in `AuthContext`.
- Refresh token moved to HttpOnly cookie flow via `app/api/auth/[...path]/route.ts`.
- 401 refresh retry works through cookie-backed refresh endpoint.

APIs:
- `/api/auth/*`
- `/api/users/*`
- `/api/orders/*`

Acceptance:
- Auth flow stable; role-aware nav and route guards are reliable.

## Phase E - Admin Surface
Status: `In Progress`

1. Admin products CRUD polish with stitch admin layout. `Done` (baseline)
2. Image upload flow reliability + validation feedback. `In Progress`
3. Admin orders page mapped for read-only first, then actions. `Done` (read-only by user-id lookup per current backend contract)
4. Admin dashboard operations cards mapped to backend health/payment ops endpoints. `Done`
5. Admin payment operations page (`/admin/payments`) and system health page (`/admin/system`). `Done`
6. Admin reviews moderation page (`/admin/reviews`). `Done`
7. Admin inventory stock lookup and update page (`/admin/inventory`). `Done`

APIs:
- `/api/products`
- `/api/products/images`
- `/api/orders`
- `/api/search/admin/relevance/dataset/health`
- `/api/payments/provider/outage-mode`
- `/api/payments/provider/dead-letters`

Acceptance:
- Admin can create/update product and see storefront reflect changes.
- Admin dashboard shows real backend operational signals.

## Phase F - Release Hardening
Status: `Pending`

1. Loading/error boundaries for all mapped pages.
2. API contract checks against gateway responses.
3. Performance pass (SSR + caching decisions).
4. Staging smoke for browse/search/cart/auth.

Acceptance:
- No placeholder data on mapped routes.
- Staging smoke passes for storefront critical paths.

## Execution Order (Strict)
1. Phase A completion verification.
2. Phase B implementation.
3. Phase C completion. (core complete)
4. Phase D completion. (complete)
5. Phase E completion.
6. Phase F hardening + beta release gate.
