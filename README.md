# KisanSetu

**A live agriculture marketplace and logistics platform connecting farmers, merchants, customers, and delivery partners.**

Built by **Team NOVA** — Kuppam Engineering College. Team lead: **T. Nanda Kumar**.

| | |
|---|---|
| 🌐 **Live app** | **https://kisansetu-green.vercel.app** |
| ❤️ **Backend health** | **https://kisansetu-backend-7mqm.onrender.com/api/health** |

---

## For Judges — Start Here

1. Open **https://kisansetu-green.vercel.app**.
2. Sign in with any demo account below — pick whichever role you want to evaluate first.
3. Follow the **3-5 minute demo sequence** in the next section.

No sign-up needed. The accounts below are real, working, dedicated demo identities — not placeholders.

### Demo accounts (verified working)

| Role | Email | Password |
|---|---|---|
| 🧑‍🌾 Farmer | `kisansetu.demo.farmer@gmail.com` | `KisanDemo@2026` |
| 🏬 Merchant | `kisansetu.demo.merchant@gmail.com` | `KisanDemo@2026` |
| 🛒 Customer | `kisansetu.demo.customer@gmail.com` | `KisanDemo@2026` |
| 🚚 Logistics | `kisansetu.demo.logistics@gmail.com` | `KisanDemo@2026` |

These are dedicated demo identities created through the app's own sign-up flow, with no elevated privilege — each is an ordinary user of its role. The shared password is intentionally public for evaluation; it is not used anywhere else. There is no administrator role in this application.

### 3-5 minute demo sequence

| Step | Role | Action | What it shows |
|---|---|---|---|
| 1 | Farmer | Sign in → **Marketplace** → view "Judge Demo Tomatoes" → create a new listing | Farmer-to-market produce listing |
| 2 | Farmer | Open **Disease Detection** → describe a symptom | AI-assisted crop advisory |
| 3 | Customer | Sign in (new tab/incognito) → **Marketplace** → search "Judge" | The farmer's listing appears instantly — real shared data, not mocked |
| 4 | Customer | Add the listing to cart → checkout | End-to-end ordering flow |
| 5 | Merchant | Sign in → **Products** → view "Judge Demo Urea" → add a product | Merchant inventory management |
| 6 | Logistics | Sign in → delivery queue → accept an assigned order | Delivery lifecycle |
| 7 | Any | Sign out, try a role-restricted page signed out | Returns a clean 401 — access control works |

---

## What KisanSetu solves

Indian farmers selling directly to customers, and sourcing inputs from merchants, currently rely on fragmented phone calls, middlemen, and no delivery coordination. KisanSetu puts all four participants — **farmer, merchant, customer, logistics partner** — into one connected platform with role-based access, so a farmer can list produce, a customer can discover and order it, and a logistics partner can fulfill the delivery, with status visible to everyone involved at every step.

## How it works

```
Browser (Next.js, Vercel)
   │  HTTP + Supabase JWT
   ▼
Spring Boot REST API (Render, Docker)
   │
   ├─► PostgreSQL via Supabase — all application data, RLS-protected
   ├─► Supabase Auth — identity, session, ES256 JWT verified via JWKS
   ├─► Groq (LLM) — crop advisory, disease assistance, chat (offline fallback if unconfigured)
   └─► Open-Meteo — weather by coordinate, short-TTL cache
```

Every request from the frontend carries a Supabase-issued JWT. The backend verifies it cryptographically (no shared secret with Supabase needed), resolves the caller's role from the `user_roles` table, and enforces `@PreAuthorize` role checks on every endpoint. A brand-new Supabase Auth identity gets its application profile and role provisioned automatically on its first authenticated request.

## Role-by-role features (verified against the live app)

### 🧑‍🌾 Farmer
- Create, edit, and manage marketplace produce listings (price, quantity, variety, harvest date, location)
- Buy inputs from merchants' product catalogs
- Accept, pack, dispatch, and track customer orders for their own produce
- AI crop advisory — free-text questions answered by an agricultural-advisory model
- Disease detection — submit a symptom description (or photo, when storage is configured) for a diagnosis, treatments, and prevention tips
- Crop planner — input soil/season/budget, get crop recommendations with expected returns
- Farming calendar — upcoming tasks, AI-suggested reminders
- View ratings received from customers
- Dashboard summarizing listings, orders, and recent activity

### 🏬 Merchant
- List and manage agricultural input products (fertilizers, seeds, pesticides, equipment) with stock thresholds
- Browse other merchants' catalogs (compare, categories, nearby)
- Receive, accept, reject, and advance farmer orders
- Dashboard with order counts and low-stock alerts

### 🛒 Customer
- Browse farmer produce (search, category filter, price range, compare)
- Browse farmer profiles, ratings, and delivered-sales count
- Personalized produce recommendations
- Cart, wishlist, and saved delivery addresses
- Checkout → order → cancel → track status through to delivery
- Rate a farmer after a delivered order (one rating per order, enforced)
- Order invoices and notifications

### 🚚 Logistics
- Queue of orders awaiting assignment and active deliveries
- Accept / reject an assigned delivery
- Advance a delivery through pickup → in-transit → delivered
- Route sequencing for multi-stop days
- Earnings tracking (commission-based, configurable percentage)
- Toggle availability (available / busy / offline)
- Dashboard with active and completed deliveries

### Shared across all roles
- Supabase Auth sign-in (email/password; Google OAuth is wired in code — see Known Limitations), session handling, sign-out
- In-app notifications (order updates, assignment alerts), unread count, mark-read
- Strict role-based access: every non-public endpoint requires a matching role; an unauthenticated or wrong-role request returns `401`/`403`, verified live

## Known limitations — stated plainly, not hidden

| Area | Status | Why |
|---|---|---|
| **AI features** (advisory, crop planner, chat, disease detection) | Working, but in **offline fallback mode** | No `AI_API_KEY` (Groq) is currently configured on the backend. The app is explicitly designed to degrade gracefully: responses are real, structured, and genuinely useful (verified live), but are template-based rather than a live LLM call. Adding a Groq key in Render's environment panel switches this on with no code change. |
| **Weather** | Returns `502` on a first request for a new coordinate | Root cause confirmed live: Open-Meteo is rate-limiting (`HTTP 429`) requests from Render's shared free-tier egress IP — Open-Meteo itself is reachable and healthy from other networks. Not a code defect. The backend has a short-TTL cache with stale-cache fallback for coordinates that have succeeded before; a from-cold-start first call for a brand-new coordinate has nothing to fall back to yet. |
| **Avatar / disease-photo image upload** | Returns `500` | `SUPABASE_SERVICE_ROLE_KEY` is not configured on the backend (intentionally withheld from automated configuration — see Environment variables). Every other feature works without it; this is the one endpoint that needs it. |
| **Google OAuth sign-in button** | Present in the UI, not fully wired in production | Supabase Authentication → URL Configuration needs the production frontend URL added as a Site URL / Redirect URL — a one-time dashboard step, not exposed through any API. Email/password sign-in (including all demo accounts) is unaffected. |
| **Administrator role** | Does not exist | The application's role model (`app_role` enum) is exactly Farmer, Merchant, Customer, Logistics. No admin account was created or implied. |

## Technology stack

| Layer | Technology |
|---|---|
| Frontend | Next.js 16 (App Router), React 19, Tailwind CSS v4 — deployed on **Vercel** |
| Backend | Spring Boot 3.3, Java 21, Maven, Flyway — deployed on **Render** (Docker) |
| Database | PostgreSQL via **Supabase**, Row-Level Security enabled on all 25 application tables |
| Auth | Supabase Auth — ES256 JWT, verified against the project's JWKS endpoint |
| AI | Groq (OpenAI-compatible API), with a deterministic offline fallback provider |
| Weather | Open-Meteo (keyless), cached per coordinate |

## Architecture notes

- The backend is a single Spring Boot modular monolith (not microservices), exposing a JSON API wrapped in a consistent `{success, data, message}` envelope.
- Database access control is layered: Supabase Row-Level Security blocks `anon`/`authenticated` Data-API access entirely (verified: zero grants remain on any of the 25 tables), and the Spring Boot backend — the only thing with a direct database credential — enforces ownership and role checks itself in the service layer.
- Flyway migrations (`V1`-`V6`) are the single source of truth for schema; all six have run successfully against the live database.

## Local development

### Prerequisites
- Java 21, Maven 3.9+
- Node.js 20+ / npm
- A Supabase project (or local PostgreSQL 14+ for schema-only work)

### Backend
```bash
cd backend
export DB_URL=jdbc:postgresql://<pooler-host>:5432/postgres
export DB_USERNAME=postgres.<project-ref>
export DB_PASSWORD=<your-database-password>
export SUPABASE_URL=https://<project-ref>.supabase.co
export SUPABASE_ANON_KEY=<anon-key>
export SUPABASE_JWT_ISSUER=https://<project-ref>.supabase.co/auth/v1
export SUPABASE_JWT_JWK_URI=https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json
mvn spring-boot:run
```
Flyway applies every migration automatically on startup. API docs: `http://localhost:8080/swagger-ui.html`.

### Frontend
```bash
cd frontend
cp .env.example .env.local   # fill in the three NEXT_PUBLIC_* values below
npm install
npm run dev        # http://localhost:3000
```

### Tests
```bash
cd backend && mvn test
```
182 tests total. 36 are database-dependent integration tests that need a local PostgreSQL at `kisansetu_test` — they report as errors (not failures) without one, meaning "didn't run," not "ran and failed." The remainder are unit tests and pass cleanly on a genuine JDK 21 toolchain (as used in Render's Docker build).

```bash
cd frontend && npm run lint && npx tsc --noEmit && npm run build
```

## Environment variables

Names only — real values belong in Render's/Vercel's dashboard environment panels, never in a committed file.

**Backend (Render):** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_JWT_SECRET` (optional, legacy HS256 only), `SUPABASE_JWT_ISSUER`, `SUPABASE_JWT_JWK_URI`, `CORS_ALLOWED_ORIGINS`, `AI_PROVIDER`, `AI_API_KEY` (optional — see Known Limitations), `WEATHER_API_KEY` (optional), `MAP_API_KEY` (optional), `PORT`.

**Frontend (Vercel):** `NEXT_PUBLIC_SUPABASE_URL`, `NEXT_PUBLIC_SUPABASE_ANON_KEY`, `NEXT_PUBLIC_API_URL`.

## Database migrations

| Version | Purpose |
|---|---|
| `V1` | Core schema — 25 tables |
| `V2` | Development seed data |
| `V3` | Removes seeded business data, keeps auth/reference rows |
| `V4` | Google OAuth columns on `profiles` |
| `V5` | Enables RLS, revokes `anon`/`authenticated` Data-API grants on all 25 tables |
| `V6` | Adds the `farmer_ratings.order_id → customer_orders.id` foreign key |

All six confirmed applied against the live database (`flyway_schema_history`, every row `success: true`).

## Troubleshooting

- **CORS error in the browser**: `CORS_ALLOWED_ORIGINS` on the backend must exactly match the frontend's origin — already set correctly in production (`https://kisansetu-green.vercel.app`, confirmed via a live preflight check).
- **Login works but no profile/role shows**: reload once after first sign-in — the profile provisions on first authenticated request.
- **`mvn test` reports many errors locally**: see the Tests section — check whether they're the 36 named integration tests (need local Postgres) or a local JDK-version artifact, neither a real defect.

---

*Demo data created for judge evaluation is clearly labeled (`kisansetu.demo.*` accounts, "Judge Demo" listings/products) and is additive — nothing in this repository deletes real data automatically.*
