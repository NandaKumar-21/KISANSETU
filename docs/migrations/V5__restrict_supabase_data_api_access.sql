-- PROPOSED - NOT APPLIED. This file lives outside backend/src/main/resources/
-- db/migration/ deliberately, so Flyway will never pick it up on its own.
-- Moving it into that folder is the action that schedules it to run, the
-- next time the backend starts against the target database. Do that only
-- after reviewing this file and running docs/database-audit.sql's queries
-- 2 and 4 to confirm the current exposure first.
--
-- Why: confirmed (grep across frontend/src) that the frontend never calls
-- supabase.from()/.storage.()/.channel() - all 25 tables are read and
-- written exclusively through the JWT-secured Spring Boot API. V1's own
-- header comment confirms this was the intended design ("RLS policies...
-- are therefore intentionally omitted"). This migration closes Supabase's
-- auto-generated REST/Data API for all 25 tables and for any table created
-- after this one, without touching the backend's own access.
--
-- Why it's safe for the backend's own connection:
--   - ENABLE ROW LEVEL SECURITY (not FORCE) never applies to the table
--     owner or a superuser/BYPASSRLS role - only to other roles like
--     anon/authenticated. Confirm via database-audit.sql query 7 that the
--     backend's DB_USERNAME is the table owner or has rolbypassrls = true
--     BEFORE applying this.
--   - The REVOKE below only removes anon/authenticated privileges; it
--     never touches whatever role Flyway/the backend connects as.
--   - ALTER DEFAULT PRIVILEGES has no `FOR ROLE` clause, so Postgres
--     applies it to whichever role actually runs this migration - the
--     same one that created the other 25 tables - with no hardcoded role
--     name needed.

ALTER TABLE profiles                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles                ENABLE ROW LEVEL SECURITY;
ALTER TABLE products                  ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_listings      ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders                    ENABLE ROW LEVEL SECURITY;
ALTER TABLE marketplace_orders        ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_addresses        ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_cart             ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_wishlist         ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_orders           ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_status_history      ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_transactions    ENABLE ROW LEVEL SECURITY;
ALTER TABLE disease_records           ENABLE ROW LEVEL SECURITY;
ALTER TABLE farmer_calendar           ENABLE ROW LEVEL SECURITY;
ALTER TABLE farmer_ratings            ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications             ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_preferences      ENABLE ROW LEVEL SECURITY;
ALTER TABLE availability_alerts       ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_invoices            ENABLE ROW LEVEL SECURITY;
ALTER TABLE delivery_partner_status   ENABLE ROW LEVEL SECURITY;
ALTER TABLE deliveries                ENABLE ROW LEVEL SECURITY;
ALTER TABLE weather_cache             ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_conversations          ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_messages               ENABLE ROW LEVEL SECURITY;
ALTER TABLE crop_plans                ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON ALL TABLES IN SCHEMA public FROM anon, authenticated;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
  REVOKE ALL ON TABLES FROM anon, authenticated;

-- No sequence grants/revokes: every table uses a UUID primary key
-- (gen_random_uuid()), so there are no Postgres SEQUENCE objects in this
-- schema (deliveries.route_sequence is an INTEGER column, not a sequence).

-- Residual process risk this migration cannot close by itself: a FUTURE
-- migration that creates a new table must add its own
-- `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` line - Postgres has no
-- "RLS on by default" switch. The ALTER DEFAULT PRIVILEGES statement above
-- is the real safety net for that case: even without RLS, a brand-new
-- table still won't be reachable via PostgREST, since anon/authenticated
-- won't have been granted anything on it by default.
