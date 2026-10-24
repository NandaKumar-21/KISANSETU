-- ============================================================
-- KISANSETU - read-only database audit
--
-- Every statement here is SELECT-only. Nothing writes to data,
-- schema, or permissions. Safe to run as-is in the Supabase SQL
-- Editor, or via any read-only Postgres client.
--
-- Run this BEFORE applying any migration under docs/migrations/.
-- ============================================================

-- ------------------------------------------------------------
-- 1. Public tables and columns
-- ------------------------------------------------------------
select table_name, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
order by table_name, ordinal_position;

-- ------------------------------------------------------------
-- 2. RLS-enabled tables
-- ------------------------------------------------------------
select schemaname, tablename,
       rowsecurity  as rls_enabled,
       forcerowsecurity as rls_forced
from pg_tables
where schemaname = 'public'
order by tablename;

-- ------------------------------------------------------------
-- 3. Existing RLS policies
-- ------------------------------------------------------------
select schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
from pg_policies
where schemaname = 'public'
order by tablename, policyname;

-- ------------------------------------------------------------
-- 4. Effective table permissions (anon / authenticated / service_role)
-- ------------------------------------------------------------
select table_schema, table_name, grantee, privilege_type
from information_schema.role_table_grants
where table_schema = 'public'
  and grantee in ('anon', 'authenticated', 'service_role')
order by table_name, grantee, privilege_type;

-- ------------------------------------------------------------
-- 5. Schema-level privileges on `public`
-- ------------------------------------------------------------
select nspname as schema_name,
       (aclexplode(nspacl)).grantee::regrole::text as grantee,
       (aclexplode(nspacl)).privilege_type
from pg_namespace
where nspname = 'public';

-- ------------------------------------------------------------
-- 6. Default privileges for FUTURE tables/sequences in `public`
-- ------------------------------------------------------------
select pg_get_userbyid(d.defaclrole) as default_for_role,
       n.nspname as schema_name,
       d.defaclobjtype as object_type,   -- 'r' = tables, 'S' = sequences
       (aclexplode(d.defaclacl)).grantee::regrole::text as grantee,
       (aclexplode(d.defaclacl)).privilege_type
from pg_default_acl d
join pg_namespace n on n.oid = d.defaclnamespace
where n.nspname = 'public'
order by default_for_role, object_type;

-- ------------------------------------------------------------
-- 7. Role capabilities - run as (or compare against) the role your
--    backend's DB_USERNAME actually connects as. rolbypassrls = true
--    or being the table owner means RLS changes won't lock out the
--    backend itself.
-- ------------------------------------------------------------
select rolname, rolbypassrls, rolsuper, rolcreaterole, rolcreatedb
from pg_roles
where rolname = current_user;

select tableowner, count(*) as table_count
from pg_tables
where schemaname = 'public'
group by tableowner;

-- ------------------------------------------------------------
-- 8. Foreign keys and indexes
-- ------------------------------------------------------------
select tc.table_name, kcu.column_name, ccu.table_name as references_table,
       ccu.column_name as references_column, rc.delete_rule
from information_schema.table_constraints tc
join information_schema.key_column_usage kcu
  on tc.constraint_name = kcu.constraint_name and tc.table_schema = kcu.table_schema
join information_schema.constraint_column_usage ccu
  on tc.constraint_name = ccu.constraint_name and tc.table_schema = ccu.table_schema
join information_schema.referential_constraints rc
  on tc.constraint_name = rc.constraint_name and tc.table_schema = rc.constraint_schema
where tc.constraint_type = 'FOREIGN KEY' and tc.table_schema = 'public'
order by tc.table_name;

select tablename, indexname, indexdef
from pg_indexes
where schemaname = 'public'
order by tablename, indexname;

-- ------------------------------------------------------------
-- 9. Flyway migration history (only exists once Flyway has run
--    against this database at least once)
-- ------------------------------------------------------------
select installed_rank, version, description, type, checksum, success, installed_on
from flyway_schema_history
order by installed_rank;

-- ------------------------------------------------------------
-- 10. Sensitive columns and current access-control coverage
--     (cross-reference against query 2 and 4 above: these tables
--     hold PII and must NOT show grants to anon/authenticated)
-- ------------------------------------------------------------
select table_name, column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name in ('profiles', 'customer_addresses', 'farmer_calendar',
                      'customer_orders', 'orders', 'marketplace_orders',
                      'disease_records', 'notifications')
  and column_name in ('full_name', 'phone', 'address', 'city', 'state',
                      'pincode', 'latitude', 'longitude', 'avatar_url',
                      'address_line', 'email', 'customer_id', 'farmer_id',
                      'merchant_id')
order by table_name, column_name;

-- ------------------------------------------------------------
-- Bonus: orphan check before adding the farmer_ratings -> customer_orders
-- FK proposed in docs/migrations/ (run this first; a non-zero count means
-- that migration cannot be applied as-is and needs remediation first)
-- ------------------------------------------------------------
select count(*) as orphaned_rating_rows
from farmer_ratings fr
where not exists (
  select 1 from customer_orders co where co.id = fr.order_id
);
