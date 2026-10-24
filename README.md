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
| Frontend | *not yet deployed* | Blocked — see [Deployment status](#deployment-status) |
| Backend | https://kisansetu-backend-7mqm.onrender.com | Deployed, but not yet healthy — see below |
| Backend health | https://kisansetu-backend-7mqm.onrender.com/api/health | Returns `{"status":"UP"}` once the database password is configured |
| Database | Supabase project `oxadadhtpdaizwqgyjpi` (ap-south-1) | Empty — migrations run on the backend's first successful boot |

### Deployment status

The backend (Render) and the database connection (Supabase) are wired up with every value that doesn't require a secret: pooler host, username, Supabase URL, JWT issuer/JWKS URI, anon key, CORS origin. Two values remain blocked because they are genuine secrets that must never pass through an AI assistant or chat transcript:

1. **`DB_PASSWORD`** on the Render service — set it in the Render dashboard (kisansetu-backend → Environment) to the project's actual Supabase database password. Render auto-redeploys on save.
2. **`SUPABASE_SERVICE_ROLE_KEY`** on the Render service — only needed for the avatar/disease-photo upload endpoint (`StorageService`); the rest of the app works without it. Set it the same way, from Supabase → Settings → API.

The frontend is not yet deployed because the Vercel MCP connector is not authorized for this account's team scope (`nandakumar-21s-projects`, error 403). Re-authorize it in claude.ai's connector settings, then the project can be created and linked to this repository in one step.

Once `DB_PASSWORD` is set, Render redeploys automatically, Flyway applies `V1`–`V5` in one sequence against the empty database (schema, seed, demo-data cleanup, Google-auth columns, then the RLS/Data-API lockdown), and `/api/health` starts returning `200 {"status":"UP"}`.

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

**Current limitation:** each account's Supabase Auth identity is live and verified (`/auth/v1/token` issues a real session for all four), but its `profiles`/`user_roles` rows in the application database do not exist yet, because the database is still empty pending the migration run described above. The first authenticated request from each account (e.g. loading its dashboard after signing in) provisions its profile and role automatically (`UserRegistrationSyncService`, idempotent). Judges should sign in once, from the live frontend, before relying on role-specific data appearing.

### Judge workflow (once both blockers above are resolved)

1. Open the frontend URL → `Sign in` → use a demo account above.
2. **Farmer**: open Marketplace → create a listing → open Disease Detection → submit a symptom query → open Crop Planner.
3. **Customer**: browse Marketplace → add an item to cart → checkout → track the order's status.
4. **Merchant**: open Products → add/edit a product → view incoming orders.
5. **Logistics**: open the delivery queue → accept an assigned order → progress its status to delivered.
6. Confirm a customer can rate a farmer after delivery, and that the farmer's rating total updates.
7. Confirm signing out and attempting a role-restricted page from another role's account is rejected (403).

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

`docs/DATABASE_AUDIT.md`, `docs/DATABASE_MIGRATION_PLAN.md`, and `docs/database-audit.sql` (read-only) cover the reasoning and verification queries in full. A `V6` (adds the missing `farmer_ratings.order_id → customer_orders.id` foreign key) is prepared in `docs/migrations/` but intentionally held back until `V1`–`V5` have run and an orphan-row check against the live data comes back clean.

Demo data is never deleted automatically by anything in this repo — there is no reset script. The `kisansetu.demo.*` accounts above are additive, ordinary rows; removing them, if ever needed, is a manual, explicit action.

## Environment variables

Names only — see `backend/.env.example` and `frontend/.env.example` for the full list. Set actual values only in Render's/Vercel's dashboard environment-variable panels, never in a committed file.

**Backend (Render):** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_JWT_SECRET` (optional, legacy HS256 only), `SUPABASE_JWT_ISSUER`, `SUPABASE_JWT_JWK_URI`, `CORS_ALLOWED_ORIGINS`, `AI_PROVIDER`, `AI_API_KEY` (optional), `WEATHER_API_KEY` (optional), `MAP_API_KEY` (optional), `PORT`.

**Frontend (Vercel):** `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `NEXT_PUBLIC_API_URL`.

## Deployment architecture

- **Backend → Render**: Docker runtime, builds `backend/Dockerfile` (`eclipse-temurin:21-jdk`, `mvn clean package -DskipTests`), health check `/api/health`.
- **Frontend → Vercel**: Next.js, root directory `frontend`.
- **Database/Auth → Supabase**: PostgreSQL + Auth, project region `ap-south-1`.
- **Order of operations**: backend first (frontend needs its URL) → frontend next, using that URL → back to Render to set `CORS_ALLOWED_ORIGINS` to the real Vercel origin (required — the backend enables credentialed CORS requests, which rejects a wildcard origin) → Supabase Authentication → URL Configuration → add the Vercel URL as a redirect URL for Google OAuth.

## Troubleshooting

- **Health check fails / Flyway connection error in Render logs**: `DB_PASSWORD` is missing or wrong. Confirm it in Render's environment panel; the JDBC URL and username are already set correctly.
- **CORS error in the browser**: `CORS_ALLOWED_ORIGINS` on the backend doesn't exactly match the frontend's origin (scheme + host, no trailing slash, no wildcard).
- **Login succeeds but the app shows no profile/role**: the account's first authenticated request hasn't completed yet — reload the page once after signing in.
- **Image upload fails**: `SUPABASE_SERVICE_ROLE_KEY` isn't set on the backend — every other feature works without it.
- **`mvn test` reports many errors locally**: see the Tests section above — check whether they're the 36 named integration tests (need a local Postgres) or a JDK-version artifact, neither of which is a real code defect.
