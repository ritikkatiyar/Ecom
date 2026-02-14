# ecom-frontend

Minimal dev frontend for testing backend flows side by side.

## Run
1. `npm install`
2. `npm run dev`
3. Open `http://localhost:5173`

By default, Vite proxies `/api/*` to `http://localhost:8080` (API Gateway).

To change backend target:
1. Create `.env`
2. Set `VITE_BACKEND_URL=http://localhost:8080`
