# KisanSetu - deployment guide

Backend -> Render (Docker). Frontend -> Vercel. Database + Auth -> Supabase.
This file names every environment variable the running code actually
reads - confirmed by grepping `application.yml` and `frontend/src` directly,
not invented. No value is filled in here; every secret stays out of the
repository.

## Order of operations

1. Deploy the backend to Render first - you need its URL for the frontend's
   `NEXT_PUBLIC_API_URL`.
2. Deploy the frontend to Vercel, using that Render URL.
3. Go back to Render and set `CORS_ALLOWED_ORIGINS` to the real Vercel URL
   (CORS requires an exact origin match - no wildcard, since the backend
   enables credentialed requests).
4. In Supabase -> Authentication -> URL configuration, add the Vercel URL
   as a Site URL / Redirect URL (required for the Google OAuth flow to
   return correctly).

## Backend: Render

Already configured via `render.yaml` at the repo root (a Render Blueprint -
Render -> New -> Blueprint -> connect this repo, it's auto-detected).

| Setting | Value | Source |
|---|---|---|
| Root directory | `backend` | `render.yaml: rootDir` |
| Runtime | Docker | `render.yaml: runtime` |
| Dockerfile | `backend/Dockerfile` | builds with its own Maven + `eclipse-temurin:21-jdk`, independent of this host's Java/Maven versions |
| Build command | none needed - the Dockerfile runs `mvn clean package -DskipTests` internally | `backend/Dockerfile` |
| Start command | `java -jar target/kisansetu-backend-2.0.0.jar` | `backend/Dockerfile` CMD, matches `pom.xml` `artifactId=kisansetu-backend` + `version=2.0.0` exactly (verified) |
| Health check path | `/api/health` | `HealthController.java`, public per `SecurityConfig.java` |

### Backend environment variables (names only - set real values in Render's dashboard)

| Variable | Purpose | Value source |
|---|---|---|
| `DB_URL` | JDBC connection string | Supabase -> Settings -> Database -> Connection string -> **Session pooler** tab, converted: `jdbc:postgresql://<pooler-host>:5432/postgres` |
| `DB_USERNAME` | DB user | Same pooler string - format is `postgres.<project-ref>` |
| `DB_PASSWORD` | DB password | The password set when the Supabase project was created |
| `SUPABASE_URL` | Project URL | Supabase -> Settings -> API |
| `SUPABASE_ANON_KEY` | Public anon key | Supabase -> Settings -> API |
| `SUPABASE_SERVICE_ROLE_KEY` | Admin key | Supabase -> Settings -> API (sensitive - never in frontend) |
| `SUPABASE_JWT_SECRET` | HS256 fallback verification | Supabase -> Settings -> API -> JWT Settings (can be left blank - current Supabase projects sign ES256, verified via JWKS below instead) |
| `SUPABASE_JWT_ISSUER` | Expected `iss` claim | `https://<project-ref>.supabase.co/auth/v1` |
| `SUPABASE_JWT_JWK_URI` | ES256 key verification endpoint | `https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json` |
| `CORS_ALLOWED_ORIGINS` | Allowed frontend origin(s) | The Vercel production URL, set after step 2 above |
| `AI_API_KEY` | Groq API key | console.groq.com (optional - offline fallback runs without it) |
| `WEATHER_API_KEY` | Alternate weather provider | Optional - Open-Meteo default needs no key |
| `MAP_API_KEY` | Alternate map provider | Optional - OSM default needs no key |

Full list with optional vars and their defaults: `backend/.env.example`.

## Frontend: Vercel

| Setting | Value |
|---|---|
| Root directory | `frontend` |
| Framework preset | Next.js (auto-detected) |
| Build command | `next build` (default) |
| Install command | `npm ci` (default, lockfile present) |

### Frontend environment variables (names only)

| Variable | Scope | Value source | Public or secret |
|---|---|---|---|
| `NEXT_PUBLIC_SUPABASE_URL` | Production, Preview, Development | Supabase -> Settings -> API, same project as the backend's `SUPABASE_URL` | Public (safe in the browser bundle by design) |
| `NEXT_PUBLIC_SUPABASE_ANON_KEY` | Production, Preview, Development | Supabase -> Settings -> API - the **anon public** key, never `service_role` | Public (safe in the browser bundle by design) |
| `NEXT_PUBLIC_API_URL` | Production, Preview, Development | The Render backend's HTTPS URL from the Backend section above | Public (it's just a URL) |

Confirmed no other environment variable is read anywhere in `frontend/src`
(`grep NEXT_PUBLIC` / `process.env` across the codebase) - only these three.
Confirmed no secret (`service_role`, `DB_PASSWORD`, `JWT_SECRET`) is ever
assigned to a `NEXT_PUBLIC_` variable, which is the only way a value would
end up in the client bundle.

## Google OAuth callback

`frontend/src/app/auth/page.tsx` builds the redirect dynamically
(`${window.location.origin}/auth`) - no hardcoded domain, so no frontend
code change is needed when the deployed domain changes. Two dashboard
settings instead:

1. Supabase -> Authentication -> Providers -> Google: enable it, paste your
   Google Cloud Console Client ID/Secret.
2. Google Cloud Console -> your OAuth client -> Authorized redirect URIs:
   add `https://<project-ref>.supabase.co/auth/v1/callback` (Supabase
   handles this callback itself - your app's own domain is never the
   OAuth redirect target).
3. Supabase -> Authentication -> URL Configuration: add the Vercel
   production URL as a Site URL / Redirect URL, so `signInWithOAuth`'s
   dynamic `redirectTo` is on the allow-list.

## Database migrations on deploy

No manual migration step. `spring.flyway.enabled: true` and
`baseline-on-migrate: true` in `application.yml` mean Flyway runs
automatically the first time the backend starts against `DB_URL` - see
`docs/DATABASE_MIGRATION_PLAN.md` for what happens in each possible
starting state of that database.

## What's already confirmed vs still open

**Confirmed in this repository, by inspection:**
- `render.yaml` blueprint exists and its env var names match
  `application.yml` exactly (checked, zero typos).
- `backend/Dockerfile`'s jar filename matches `pom.xml`'s
  `artifactId`/`version` exactly.
- `/api/health` is public and returns only `{"status":"UP"}` - no stack
  trace, no config, no credentials in the response (`HealthController.java`).
- Frontend build succeeds, 35 routes (33 static, 2 dynamic), 0 lint errors,
  0 `npm audit` vulnerabilities (re-verified this session).

**Still open / needs you:**
- No live Render or Vercel deployment exists yet (confirmed via GitHub's
  Deployments API returning zero records tied to this repo).
- Backend `mvn test` / `mvn clean package` results - see the final report
  for this session's actual run.
- `docs/migrations/V5` and `V6` are proposed only, pending your review and
  the live-database checks each one depends on.
