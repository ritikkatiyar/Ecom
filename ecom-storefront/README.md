# ecom-storefront

Next.js storefront for the e-commerce platform. Design follows the **stitch** mockups (Voluspa / Essence style).

## Tech Stack

- Next.js 15 (App Router)
- TypeScript
- Tailwind CSS v4
- @tanstack/react-query
- Newsreader + Inter fonts
- Material Symbols icons

## Run

```bash
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Backend

By default, `/api/*` requests are proxied to `http://localhost:8080` (API Gateway). Override via:

```bash
NEXT_PUBLIC_BACKEND_URL=http://localhost:8080
NEXT_PUBLIC_FEEDBACK_EMAIL=feedback@yoursite.com  # Optional; beta feedback form mailto recipient
NEXT_PUBLIC_GA_MEASUREMENT_ID=G-XXXXXXXXXX       # Optional; Google Analytics 4 measurement ID
NEXT_PUBLIC_PLAUSIBLE_DOMAIN=yoursite.com        # Optional; Plausible Analytics domain
```

- Auth: Bearer token, 401 refresh retry (apiClient and fetchWithAuthRetry for image upload).

## Routes

- `/` – Home
- `/shop` – Product listing
- `/products/[id]` – Product detail (stitch PDP)
- `/search` – Search
- `/cart` – Cart
- `/checkout` – Checkout
- `/login`, `/signup` – Auth
- `/account` – Account (protected, USER/ADMIN)
- `/admin/dashboard` – Admin (protected, ADMIN only)
- `/unauthorized` – Insufficient role
- `/collections` – Collections
- `/feedback` – Beta feedback form (mailto)

## Design

Based on `stitch/` folder:

- `voluspa_product_page_1` – PDP layout
- `admin_dashboard` – Admin UI (future)
- `user_account_page` – Account (future)

Primary: `#2badee` | Background: `#F8F6F3` | Ivory: `#EFEBE7`
