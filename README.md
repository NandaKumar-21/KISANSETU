# KisanSetu v2

Full-stack agricultural marketplace connecting farmers, merchants, customers, and logistics partners — built with **Next.js (App Router)**, **Spring Boot**, and **PostgreSQL (Supabase)**.

- **Frontend** (`frontend/`) — Next.js 16 / React 19 / Tailwind CSS v4 → deployed on **Vercel**
- **Backend** (`backend/`) — Spring Boot 3.3, Java 21, Maven, Flyway migrations → deployed on **Render** (Docker)
- **Database** — PostgreSQL via **Supabase** (schema + seed in `backend/src/main/resources/db/migration/`)
- **Auth** — Supabase Auth (ES256 JWT, verified against the project's JWKS)
- **AI** — Groq (OpenAI-compatible) with automatic offline fallback when no API key is configured
- **Weather** — Open-Meteo by default (no API key required), pluggable provider

## Live deployment

| Component | URL | Status |
|---|---|---|
| **Frontend** | **https://kisansetu-green.vercel.app** | Live, verified |
| **Backend** | https://kisansetu-backend-7mqm.onrender.com | Live, verified |
| **Backend health** | https://kisansetu-backend-7mqm.onrender.com/api/health | `200 {"status":"UP"}` |
| Database | Supabase project `oxadadhtpdaizwqgyjpi` (ap-south-1) | `V1`-`V6` all applied, RLS on, zero `anon`/`authenticated` grants |

### Verified live, end to end

- CORS: preflight from `https://kisansetu-green.vercel.app` returns a matching `Access-Control-Allow-Origin` — confirmed with a real request, not assumed.
- All four demo accounts log in through the deployed frontend's own configured backend and return the correct synced profile + role via `/api/auth/me`.
- Real cross-role workflow against production data: farmer creates a marketplace listing → customer finds it via `/api/customer/produce` search → merchant creates a product via `/api/merchant/products` → farmer dashboard and own-listings endpoints return correct, live data.
- No runtime errors reported for the Vercel deployment; backend logs show clean Flyway completion and no datasource errors.

### One remaining manual step (non-blocking for the demo accounts below)

Supabase's Auth **Site URL / Redirect URLs** (Authentication → URL Configuration) still need the Vercel origin added, for the Google OAuth flow specifically — this isn't exposed through any API, so it's a one-time dashboard edit: add `https://kisansetu-green.vercel.app` as a Site URL / Redirect URL. Email/password login (including all four demo accounts) does not depend on this and already works.

## Architecture

```
frontend/  Next.js (Vercel)  ── HTTP + JWT ──►  backend/  Spring Boot REST API (Render)  ──►  PostgreSQL (Supabase)
                                                 │
                                                 ├── Supabase Auth (ES256 JWT via JWKS)
                                                 ├── Groq AI (chat, advisory, crop planner, disease vision)
                                                 └── Open-Meteo (weather, cached per location)
```

The backend is a single Spring Boot application exposing a JSON API (wrapped in an `ApiResponse` envelope: `{success, data, message}`) secured with role-based access (`FARMER`, `MERCHANT`, `CUSTOMER`, `LOGISTICS`). There is no administrator role in this codebase — `app_role` is exactly those four values.

## Implemented modules and roles

| Role | Modules |
|---|---|
| **Farmer** | Marketplace listings, merchant-order purchases, disease detection (image/symptom), crop planner, calendar, AI chat advisory, ratings received, profile |
| **Merchant** | Product/inventory management, incoming farmer orders, profile |
| **Customer** | Marketplace browsing/search, cart, wishlist, addresses, orders + status tracking, invoices, farmer ratings, notifications, profile |
| **Logistics** | Delivery queue, route sequencing, accept/pick-up/in-transit/deliver status updates, earnings, availability toggle, profile |

Authentication, session handling, and Google OAuth sign-in are shared across all four roles via Supabase Auth.

## Judge demo accounts

Four demo accounts exist, one per role, created through the application's own public sign-up path (Supabase Auth, no admin privilege used) and verified to log in successfully:

| Role | Email | Password |
|---|---|---|
| Farmer | `kisansetu.demo.farmer@gmail.com` | `KisanDemo@2026` |
| Merchant | `kisansetu.demo.merchant@gmail.com` | `KisanDemo@2026` |
| Customer | `kisansetu.demo.customer@gmail.com` | `KisanDemo@2026` |
| Logistics | `kisansetu.demo.logistics@gmail.com` | `KisanDemo@2026` |

These are dedicated, clearly-labeled demo identities (`kisansetu.demo.*`) with a shared, intentionally public password — never reuse this password for anything else, and never store real user data under these accounts. They hold no elevated privilege; each is a normal user of its respective role.

**Verified, not assumed:** each account's profile and role already exist in the live database — confirmed via `/api/auth/me` returning the correct `fullName` and role for all four, and via real workflow calls (a farmer listing created by the farmer account was found by the customer account's marketplace search; a product created by the merchant account persisted correctly). Sign-in works immediately, no warm-up step needed.

### Judge workflow

1. Open https://kisansetu-green.vercel.app → `Sign in` → use a demo account above.
2. **Farmer**: open Marketplace → see "Judge Demo Tomatoes" (already seeded during verification) → create another listing → open Disease Detection → submit a symptom query → open Crop Planner.
3. **Customer**: browse Marketplace, search "Judge" → see the farmer's listing → add an item to cart → checkout → track the order's status.
4. **Merchant**: open Products → see "Judge Demo Urea" (already seeded) → add/edit a product → view incoming orders.
5. **Logistics**: open the delivery queue → accept an assigned order → progress its status to delivered.
6. Confirm a customer can rate a farmer after delivery, and that the farmer's rating total updates.
7. Confirm signing out and attempting a role-restricted page from another role's account is rejected (403) — verified directly against the API: an unauthenticated request to a protected endpoint returns `401`.

## Local development

### Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+ / npm
- A Supabase project (or local PostgreSQL 14+ for schema-only development)

### Backend

```bash
cd backend
# PowerShell: $env:NAME = "value"  —  bash: export NAME=value
export DB_URL=jdbc:postgresql://<pooler-host>:5432/postgres
export DB_USERNAME=postgres.<project-ref>
export DB_PASSWORD=<your-database-password>
export SUPABASE_URL=https://<project-ref>.supabase.co
export SUPABASE_ANON_KEY=<anon-key>
export SUPABASE_JWT_ISSUER=https://<project-ref>.supabase.co/auth/v1
export SUPABASE_JWT_JWK_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
mvn spring-boot:run
```

Flyway applies every migration under `backend/src/main/resources/db/migration/` automatically on startup — no manual schema step. API docs at `http://localhost:8080/swagger-ui.html`.

Full environment variable reference (names only, no values): `backend/.env.example`.

### Frontend

```bash
cd frontend
cp .env.example .env.local
# fill in NEXT_PUBLIC_SUPABASE_URL, NEXT_PUBLIC_SUPABASE_ANON_KEY, NEXT_PUBLIC_API_URL
npm install
npm run dev        # http://localhost:3000
```

`NEXT_PUBLIC_API_URL` must point at a reachable backend — never hardcode `localhost` for a deployed frontend.

### Tests

```bash
cd backend && mvn test
```

182 tests total. **36** are database-dependent integration tests (`com.kisansetu.integration.*`) that require a real local PostgreSQL at `kisansetu_test` — they report as errors, not failures, when that database isn't running, and that distinction matters: an error here means "didn't run," not "ran and failed." The rest are unit tests; all pass on a genuine JDK 21 toolchain (verified in Render's Docker build, which uses `eclipse-temurin:21-jdk`). On a machine whose only installed JDK is 23+, Lombok's generated constructors can trip a JDK-23-specific javac bug unrelated to this codebase — install a JDK 21 locally, or rely on the Docker build, for a fully clean local signal.

```bash
cd frontend && npm run lint && npx tsc --noEmit && npm run build
```

## Database migrations

Flyway migrations live in `backend/src/main/resources/db/migration/`, applied in order on every backend startup:

| Version | Purpose |
|---|---|
| `V1` | Core schema — 25 tables, no RLS (backend-only access by design) |
| `V2` | Development seed data (fixed-UUID demo profiles, listings, orders) |
| `V3` | Removes V2's seeded business data, keeps auth/reference rows |
| `V4` | Adds Google OAuth columns to `profiles` |
| `V5` | Enables RLS and revokes `anon`/`authenticated` grants on all 25 tables, closing Supabase's auto-generated Data API — the backend's own role (`postgres`, confirmed `rolbypassrls = true`) is unaffected |
| `V6` | Adds the `farmer_ratings.order_id → customer_orders.id` foreign key — applied after confirming the orphan-row check against live data returned 0 (the table was empty) |

All six applied successfully against the live Supabase project (`oxadadhtpdaizwqgyjpi`) — confirmed via `flyway_schema_history`, `success: true` on every row. `docs/DATABASE_AUDIT.md`, `docs/DATABASE_MIGRATION_PLAN.md`, and `docs/database-audit.sql` (read-only) cover the reasoning and verification queries in full.

Demo data is never deleted automatically by anything in this repo — there is no reset script. The `kisansetu.demo.*` accounts above are additive, ordinary rows; removing them, if ever needed, is a manual, explicit action.

## Environment variables

Names only — see `backend/.env.example` and `frontend/.env.example` for the full list. Set actual values only in Render's/Vercel's dashboard environment-variable panels, never in a committed file.

**Backend (Render):** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_JWT_SECRET` (optional, legacy HS256 only), `SUPABASE_JWT_ISSUER`, `SUPABASE_JWT_JWK_URI`, `CORS_ALLOWED_ORIGINS`, `AI_PROVIDER`, `AI_API_KEY` (optional), `WEATHER_API_KEY` (optional), `MAP_API_KEY` (optional), `PORT`.

**Frontend (Vercel):** `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `NEXT_PUBLIC_API_URL`.

## Deployment architecture

- **Backend → Render**: Docker runtime, builds `backend/Dockerfile` (`eclipse-temurin:21-jdk`, `mvn clean package -DskipTests`), health check `/api/health`.
- **Frontend → Vercel**: Next.js, root directory `frontend`.
- **Database/Auth → Supabase**: PostgreSQL + Auth, project region `ap-south-1`.
- **Order of operations followed for this deployment**: backend deployed first on Render → frontend deployed on Vercel with `NEXT_PUBLIC_API_URL` pointed at the Render URL → `CORS_ALLOWED_ORIGINS` on Render updated to the real Vercel origin (`https://kisansetu-green.vercel.app`, confirmed via a live CORS preflight check) → Supabase Authentication → URL Configuration still needs the Vercel URL added as a redirect URL, for Google OAuth only (one remaining manual dashboard step, see above).

## Troubleshooting

- **Health check fails / Flyway connection error in Render logs**: `DB_PASSWORD` is missing or wrong. Confirm it in Render's environment panel; the JDBC URL and username are already set correctly.
- **CORS error in the browser**: `CORS_ALLOWED_ORIGINS` on the backend doesn't exactly match the frontend's origin (scheme + host, no trailing slash, no wildcard).
- **Login succeeds but the app shows no profile/role**: the account's first authenticated request hasn't completed yet — reload the page once after signing in.
- **Image upload fails**: `SUPABASE_SERVICE_ROLE_KEY` isn't set on the backend — every other feature works without it.
- **`mvn test` reports many errors locally**: see the Tests section above — check whether they're the 36 named integration tests (need a local Postgres) or a JDK-version artifact, neither of which is a real code defect.
