# KisanSetu v2

Full-stack agricultural marketplace connecting farmers, merchants, customers, and logistics partners — built with **Next.js (App Router)**, **Spring Boot**, and **PostgreSQL**.

- **Frontend** (`frontend/`) — Next.js 16 / React 19 / Tailwind CSS v4
- **Backend** (`backend/`) — Spring Boot 3.3, Java 21, Maven, Flyway migrations
- **Database** — PostgreSQL (schema + seed in `backend/src/main/resources/db/migration/`)
- **Auth** — Supabase Auth (JWT HS256 verified server-side), or any issuer issuing `sub` + `role` claims
- **AI** — Groq (OpenAI-compatible) with automatic offline fallback when no API key is configured
- **Weather** — Open-Meteo by default (no API key required), pluggable provider

## Architecture

```
frontend/  Next.js SPA  ── HTTP + JWT ──►  backend/  Spring Boot REST API  ──►  PostgreSQL
                                            │
                                            ├── Supabase Auth (JWT secret / issuer / JWKS)
                                            ├── Groq AI (chat, advisory, crop planner, disease vision)
                                            └── Open-Meteo (weather, cached per location)
```

The backend is a single Spring Boot application exposing a JSON API (wrapped in an `ApiResponse` envelope: `{success, data, message}`) secured with role-based access (`FARMER`, `MERCHANT`, `CUSTOMER`, `LOGISTICS`).

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ / npm
- PostgreSQL 14+ running locally on `localhost:5432`

## Backend setup

### 1. Create the database

```sql
CREATE DATABASE kisansetu;
CREATE DATABASE kisansetu_test;   -- used by the integration test suite
```

Flyway applies schema (`V1__init_schema.sql`) and seed data (`V2__seed_data.sql`, idempotent) automatically at startup.

### 2. Configuration (environment variables)

| Variable | Default | Description |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated origins |
| `SUPABASE_URL` | *(empty)* | Supabase project URL |
| `SUPABASE_ANON_KEY` | *(empty)* | Supabase anon key |
| `SUPABASE_SERVICE_ROLE_KEY` | *(empty)* | Supabase service role key |
| `SUPABASE_JWT_SECRET` | *(empty)* | Supabase JWT secret (HS256 signing secret) — **required to verify tokens** |
| `SUPABASE_JWT_ISSUER` | `https://your-project.supabase.co/auth/v1` | Expected `iss` claim; only enforced when set to a real issuer |
| `SUPABASE_JWT_JWK_URI` | `https://your-project.supabase.co/auth/v1/.well-known/jwks.json` | JWKS endpoint (used when the JWT secret is empty) |
| `AI_PROVIDER` | `groq` | AI provider (`groq` or `offline`) |
| `AI_API_KEY` | *(empty)* | Groq API key; empty ⇒ offline fallback provider |
| `AI_MODEL` | `meta-llama/llama-4-scout-17b-16e-instruct` | Model name |
| `AI_BASE_URL` | `https://api.groq.com/openai/v1` | OpenAI-compatible base URL |
| `AI_TIMEOUT_SECONDS` | `60` | Provider timeout |
| `AI_MAX_TOKENS` | `2048` | Max completion tokens |
| `WEATHER_PROVIDER` | `open-meteo` | Weather provider |
| `WEATHER_BASE_URL` | `https://api.open-meteo.com/v1` | Weather endpoint (keyless) |
| `WEATHER_API_KEY` | *(empty)* | Optional provider key |
| `LOGISTICS_EARNING_PERCENTAGE` | `5.0` | Partner commission % of order value |
| `DEFAULT_CITY` | *(empty)* | Default logistics city |
| `STORAGE_BUCKET` | `kisansetu` | Supabase storage bucket |
| `MAP_PROVIDER` | `osm` | Map provider |
| `MAP_API_KEY` | *(empty)* | Map provider key |

### 3. Run

```bash
cd backend
export SUPABASE_JWT_SECRET=your-supabase-jwt-secret
export DB_URL=jdbc:postgresql://localhost:5432/kisansetu
export DB_USERNAME=postgres
export DB_PASSWORD=your-password
mvn spring-boot:run
```

API docs: `http://localhost:8080/swagger-ui.html` (OpenAPI at `/v3/api-docs`).

### 4. Tests

```bash
cd backend
mvn test
```

**163 tests, all green** — 128 unit tests (services, JWT decoding, state machines, GeoUtil) and 35 integration/E2E tests that boot the full Spring context on a random port against a real PostgreSQL database (`kisansetu_test`), including a complete order lifecycle:

farmer lists produce → customer orders → farmer confirms/packs/dispatches → logistics partner delivers → customer rates → earnings & notifications update.

The test JWT secret is `test-secret-0123456789-abcdefghij` (`src/test/resources/application-test.yml` + `TestJwt.java`).

## Frontend setup

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev        # http://localhost:3000
```

Environment variables (`.env.local`):

| Variable | Purpose |
|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | Supabase project URL (`https://<project>.supabase.co`) |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Supabase anon key |
| `NEXT_PUBLIC_API_URL` | Backend base URL (`http://localhost:8080`) |

The frontend degrades gracefully: with placeholder values it renders fully and shows a "Supabase is not configured" notice on the auth page instead of crashing.

## Supabase configuration

1. Create a project at [supabase.com](https://supabase.com).
2. In **Authentication → Providers**, leave email/password enabled.
3. Copy the project URL, anon key, and service role key from **Settings → API** into `.env.local` and the backend env vars.
4. In **Settings → API → JWT settings**, copy the JWT secret into `SUPABASE_JWT_SECRET` (backend). The backend verifies HS256-signed tokens itself; no edge functions or gateway are required.
5. Create the demo users so JWT `sub` values match the seed profiles (`db/demo/demo-users.sql` pattern — each seeded profile's `user_id` is a fixed UUID like `a0000000-...-000000000001`). Sign-in works once the profile row exists for the authenticated `sub`.

Roles are derived from the `role` claim or the `user_roles` table; the seed assigns each demo user its role.

## AI and weather configuration

- **AI without a key:** leave `AI_API_KEY` empty — the backend switches to an offline provider that returns structured, deterministic responses for crop advisory, disease analysis, and the crop planner. Verified by integration tests.
- **AI with Groq:** set `AI_API_KEY` (optionally `AI_MODEL`, `AI_BASE_URL`) — chat, disease detection (image + symptom), and crop planning go to Groq.
- **Weather:** default `open-meteo` needs no key; responses are cached per coordinate with stale-cache fallback when the provider is unreachable.

## Demo data

Seed data (`V2__seed_data.sql`) creates realistic profiles, roles, marketplace listings, merchant products, customer addresses, and a few orders. Example demo accounts:

| Role | Name | user_id suffix |
|---|---|---|
| Farmer | Ramesh Patil | `a0000000-...-000000000001` |
| Farmer | Sunita Devi | `a0000000-...-000000000002` |
| Merchant | Kisan Agro Centre | `a0000000-...-000000000011` |
| Customer | Priya Sharma | `a0000000-...-000000000021` |
| Logistics | Ravi Kumar | `a0000000-...-000000000031` |

## Verification status

- Backend: 163 tests pass (`mvn test`, includes full E2E lifecycle against real PostgreSQL).
- Frontend: `tsc --noEmit` clean, `npm run build` succeeds, 35 static routes verified.
- Core flows verified end-to-end: marketplace ordering, farmer order pipeline, logistics delivery pipeline, ratings, earnings, notifications, cart/wishlist/addresses, security (401/403/ownership rules), AI fallback behavior.

`repo-analysis/` contains the original prototype for reference only and is not part of the v2 build.