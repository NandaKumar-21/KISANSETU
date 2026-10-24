# KisanSetu - database initialization procedure

This describes what to do for each possible starting state of the target
Postgres database (your new Supabase project's database). Flyway migrations
under `backend/src/main/resources/db/migration/` (V1-V4) are the source of
truth for schema - nothing here invents a replacement schema.

## Step 0 (always first): run the read-only audit

Run every query in `docs/database-audit.sql` in the Supabase SQL Editor (or
any read-only Postgres client) before anything else. It tells you which
case below you're actually in:

- Query 9 (`flyway_schema_history`) either returns rows, returns "relation
  does not exist", or returns an empty result.
- Query 1 (`information_schema.columns`) either lists the 25 expected
  tables, lists some other/partial set, or returns nothing.

## Case A: brand-new, empty database (your situation - new Supabase project)

Nothing to do manually. The moment the backend starts up with `DB_URL`
pointed at this database (`spring.flyway.enabled: true`,
`baseline-on-migrate: true` in `application.yml`), Flyway will:

1. Create its own `flyway_schema_history` tracking table.
2. Apply `V1__init_schema.sql` (schema), `V2__seed_data.sql` (demo seed
   data), `V3__remove_demo_business_data.sql`, `V4__add_google_auth_fields.sql`
   in order.

No manual SQL step is required for this case. Validate afterward with:

```sql
select version, description, success from flyway_schema_history order by installed_rank;
```

All four rows should show `success = true`.

## Case B: existing database with the expected schema already present

If `flyway_schema_history` already has V1-V4 marked successful, starting
the backend does nothing further - Flyway is a no-op once a migration's
checksum matches what's already recorded. No action needed.

## Case C: partially migrated (some tables exist, Flyway history is incomplete or missing)

This is the one case that needs care. Do not let Flyway blindly re-run V1
against a database that already has some of these tables - `CREATE TABLE IF
NOT EXISTS` would skip re-creating existing ones, but Flyway needs to agree
it has already "seen" V1, or it will try to apply it as new and may hit a
partial failure if the two drift.

1. Run the audit SQL, specifically query 1, and compare the resulting table
   list against the 25 named in `docs/DATABASE_AUDIT.md`.
2. If the live schema matches V1 exactly (same 25 tables, same columns) but
   `flyway_schema_history` is missing or doesn't list V1: use Flyway's
   `baseline` command (already enabled via `baseline-on-migrate: true`) -
   starting the backend against this database will have Flyway baseline
   itself at the current state rather than re-running V1, *provided* the
   schema truly matches. If it doesn't match exactly, do not let this
   happen silently - fix the discrepancy first (see below) and only then
   let the backend start.
3. If the live schema is a genuine partial subset (e.g. only some of the 25
   tables exist, from an interrupted earlier attempt): do not drop and
   recreate. Compare what's missing against V1's `CREATE TABLE IF NOT
   EXISTS` statements and either let Flyway apply V1 (safe - it skips
   tables that already exist and only creates what's missing) or, if you
   need finer control, apply the missing pieces manually with the exact
   DDL from `V1__init_schema.sql`, then baseline Flyway at V1 afterward.

## Case D: existing schema, Flyway history missing or inconsistent (checksum mismatch)

If `flyway_schema_history` exists but a stored checksum for V1-V4 doesn't
match the current file content, Flyway will refuse to proceed by default
(`validate-on-migrate` behavior) rather than silently continuing - this is
Flyway protecting you, not a bug to work around. Do not modify an
already-applied migration file to make a checksum match - per the project
rule, modifying an applied migration is not an option. Instead:

1. Identify exactly which version's checksum differs and why (did the file
   change after it was applied somewhere else? was the DB migrated by a
   different means?).
2. If the live schema is actually correct and the checksum mismatch is
   spurious (e.g. line-ending differences from a different checkout), use
   Flyway's `repair` command to recompute the stored checksum against the
   current file - `repair` only touches Flyway's own bookkeeping table,
   never your data or schema.
3. If the live schema is genuinely different from what V1-V4 describe,
   that needs a forward-only corrective migration (a new `V7__...sql`),
   never an edit to V1-V4.

## What NOT to do, regardless of case

- Never drop/recreate an existing table just to make Flyway pass.
- Never delete production data to resolve a migration conflict.
- Never run `flyway repair` to paper over a real schema mismatch - only use
  it when the live schema is already correct and only the checksum
  bookkeeping is wrong.
- Never edit V1-V4 in place once applied anywhere - always add a new
  forward-only migration.

## Pending migrations awaiting your approval (not yet moved into the real migration folder)

Both sit in `docs/migrations/` right now, which Flyway does not scan - they
do nothing until moved into `backend/src/main/resources/db/migration/`:

- `V5__restrict_supabase_data_api_access.sql` - enables RLS + revokes
  anon/authenticated grants on all 25 tables. Run `docs/database-audit.sql`
  query 7 first to confirm your backend's DB role is the table owner or has
  `rolbypassrls = true`, so this can't lock out the backend itself.
- `V6__add_farmer_ratings_order_fk.sql` - adds the missing foreign key from
  `farmer_ratings.order_id` to `customer_orders.id`. Run the orphan-check
  query at the bottom of `docs/database-audit.sql` first; a non-zero count
  means this needs manual remediation before it can apply.

Tell me to move either one into the real migration folder once you've
reviewed it and run the corresponding check - that move is the action that
schedules it to run, the next time the backend starts against the target
database.
