# V1 Scope and Feature Flags

## Must Have (V1)
- Product browse/listing
- Product search
- Cart operations (guest + logged-in)
- Auth login/signup/refresh/logout
- Basic order creation flow

## Feature-Flagged (Post-V1 or Controlled Rollout)
- Admin console UI
- Advanced payment outage controls in UI
- Deep review moderation UI
- Notification operations dashboard
- Experimental ranking controls

## Frontend Flags
- `VITE_FLAG_BETA_BANNER` (default: true)
- `VITE_FLAG_ADMIN_CONSOLE` (default: false)

## Backend Flags (Gateway internal endpoint)
- `app.frontend-flags.beta-banner-enabled`
- `app.frontend-flags.admin-console-enabled`
