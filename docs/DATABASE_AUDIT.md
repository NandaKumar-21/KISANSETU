# KisanSetu - database audit

Static code review of `backend/src/main/resources/db/migration/` (V1-V4) and the
JPA entities that map to it. No live database connection was available at the
time of this audit (no Postgres/Supabase CLI authenticated in this
environment) - every finding below is from reading the migration SQL and
Java source directly. Run `docs/database-audit.sql` against the real
database to confirm live state before acting on anything here.

## Schema overview

25 tables, created entirely in `V1__init_schema.sql`. `V2` seeds demo data,
`V3` removes demo business data, `V4` adds Google-auth columns to `profiles`.
No later migration adds, drops, or renames a table - the 25-table list is
final as of the current migration set.

`profiles`, `user_roles`, `products`, `marketplace_listings`, `orders`,
`marketplace_orders`, `customer_addresses`, `customer_cart`,
`customer_wishlist`, `customer_orders`, `order_status_history`,
`inventory_transactions`, `disease_records`, `farmer_calendar`,
`farmer_ratings`, `notifications`, `customer_preferences`,
`availability_alerts`, `order_invoices`, `delivery_partner_status`,
`deliveries`, `weather_cache`, `ai_conversations`, `ai_messages`,
`crop_plans`.

Flyway versions V1-V4 are sequential with no gaps or duplicates. `CREATE
TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, and `DO $$ ... EXCEPTION
WHEN duplicate_object` guards make the schema migration safely re-runnable,
which is defensive rather than necessary (Flyway itself tracks what's
already applied) but causes no harm.

## RLS and access control - intentional, not an oversight

`V1__init_schema.sql`'s own header comment states the design directly:

> The Spring Boot backend is the only application talking to this database;
> RLS policies from the original Supabase-only app are therefore
> intentionally omitted (authorization is enforced in Spring Security +
> service layer ownership checks instead).

This matches what the code shows: zero `supabase.from()`, `.storage.*`, or
`.channel()` calls anywhere in `frontend/src` - the Supabase client is used
only for Auth. **No table currently has RLS enabled, and no policy exists.**
Given the confirmed access pattern (backend-only), this is a real exposure
risk via Supabase's auto-generated REST API, not a false alarm - see
`docs/migrations/V5__restrict_supabase_data_api_access.sql` (proposed, not
applied) and run `docs/database-audit.sql` queries 2 and 4 to confirm the
live grant state before deciding whether to apply it.

## Foreign keys and relationships

Most FKs are present and correct: `orders.product_id`, `marketplace_orders
.listing_id`, `customer_cart.listing_id`, `customer_wishlist.listing_id`,
`inventory_transactions.product_id`, `order_invoices.order_id`,
`deliveries.order_id`, `ai_messages.conversation_id` all reference the
right parent table with sensible `ON DELETE` behavior (cascading for
dependent rows, restricting where history must be preserved - e.g.
`customer_orders.listing_id` has no cascade, so a listing can't be
hard-deleted out from under order history; the app uses `listing_status`
for lifecycle instead of deletion).

`user_id`, `farmer_id`, `merchant_id`, `customer_id`, `buyer_id`,
`partner_id`, `changed_by`, `created_by` columns are deliberately NOT
foreign-keyed to anything in this schema - they reference Supabase Auth's
`auth.users.id`, which lives in a different schema this migration set
doesn't own. Correct as designed.

`order_status_history.order_id` is polymorphic across 4 order tables (via
an `order_type` CHECK column) - no single FK is possible for that pattern.
Accepted trade-off for a shared audit-log table, not a defect.

### Finding 1 (real, confirmed): missing FK on `farmer_ratings.order_id`

`farmer_ratings` has `order_id UUID NOT NULL` with a `UNIQUE (order_id)`
constraint, and the app's own rating flow (customer rates a farmer after a
`customer_orders` delivery) clearly intends it to reference
`customer_orders(id)` - but no FK was ever added. Nothing at the database
level currently prevents a `farmer_ratings` row pointing at a nonexistent
order. Proposed fix: `docs/migrations/V6__add_farmer_ratings_order_fk.sql`
(forward-only, additive). Run the orphan-check query at the bottom of
`docs/database-audit.sql` first - a non-zero result means orphaned rows
need manual remediation before this migration can apply cleanly.

### Observation (not a migration-level fix): delivery status duplication

`customer_orders.delivery_status` and `deliveries.status` both store what
is logically the same state (`delivery_status` enum), in two different
tables. This is either intentional denormalization (a fast-read cache on
`customer_orders`, kept in sync by the service layer whenever `deliveries
.status` changes) or an unintentional split that could drift out of sync -
I did not trace every service-layer write path to determine which. Not
something to fix via SQL migration without first confirming the intended
behavior in `OrderService`/the logistics service layer.

## Data types vs Java entities

Spot-checked `CustomerOrder.java` against `customer_orders`: `BigDecimal`
for every `NUMERIC`/`DECIMAL` column, `Instant` for every `TIMESTAMPTZ`
column (consistent with `hibernate.jdbc.time_zone: UTC` in
`application.yml`), and `@Enumerated(EnumType.STRING)` combined with
`@JdbcType(PostgreSQLEnumJdbcType.class)` for the two native Postgres enum
columns (`status`, `delivery_status`) - the correct way to map a Java enum
to a native Postgres `ENUM` type rather than a plain `VARCHAR`. No
mismatch found in this entity. (Minor, harmless redundancy: both
`@PrePersist`/`@PreUpdate` in Java and a `BEFORE UPDATE` trigger in V1 set
`updated_at` independently - the trigger is authoritative and this causes
no incorrect behavior, just double-setting the same value.)

## Schema ownership and privileges

Cannot be determined without a live connection - run `docs/database-audit.sql`
query 7 (role capabilities / `rolbypassrls`) and report back which role
your backend's `DB_USERNAME` actually is, and whether it owns these 25
tables. This matters directly for whether `V5` (RLS enable) is safe to
apply without affecting the backend's own access.

## Flyway migration history

Cannot be checked without a live connection - `docs/database-audit.sql`
query 9 reads `flyway_schema_history` directly, which only exists once
Flyway has run against a given database at least once. See
`docs/DATABASE_MIGRATION_PLAN.md` for what to do in each possible starting
state (empty database, partially migrated, or history missing/inconsistent).
