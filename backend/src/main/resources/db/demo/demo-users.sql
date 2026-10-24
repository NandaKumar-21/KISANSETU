-- ============================================================
-- KisanSetu v2 — Demo auth users
-- Run this in the Supabase Dashboard > SQL Editor AFTER migrations.
--
-- Strategy: synchronize BY EMAIL, never by hardcoded UUIDs.
--
-- Part 1 creates auth.users rows whose IDs EXACTLY match the seeded
-- profiles.user_id values (a0000000-...) for demo emails that do not
-- exist yet.
--
-- Part 2 handles demo emails that ALREADY exist in Supabase Auth with
-- a random UUID (created via the app signup screen or the Dashboard):
-- it copies the demo role + name into their user metadata, then creates
-- the matching profiles / user_roles rows using their ACTUAL id.
--
-- The backend also auto-provisions profiles and user_roles from the
-- verified JWT user_metadata on the first authenticated request, so
-- accounts created through the app's signup screen work with no manual
-- SQL at all. This script is only needed for pre-existing accounts.
-- ============================================================

-- ---------- Part 1: create missing demo auth users (fixed ids) ----------
INSERT INTO auth.users (id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role, created_at, updated_at)
SELECT * FROM (VALUES
  ('a0000000-0000-4000-8000-000000000001'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'farmer.ramesh@kisansetu.demo', crypt('farmer123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Ramesh Patil","role":"farmer"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000002'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'farmer.sunita@kisansetu.demo', crypt('farmer123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Sunita Devi","role":"farmer"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000003'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'farmer.arjun@kisansetu.demo', crypt('farmer123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Arjun Singh","role":"farmer"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000004'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'farmer.lakshmi@kisansetu.demo', crypt('farmer123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Lakshmi Reddy","role":"farmer"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000011'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'merchant.kisanagro@kisansetu.demo', crypt('merchant123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Kisan Agro Centre","role":"merchant"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000012'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'merchant.greenfield@kisansetu.demo', crypt('merchant123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"GreenField Seeds","role":"merchant"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000013'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'merchant.agrocare@kisansetu.demo', crypt('merchant123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"AgroCare Supplies","role":"merchant"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000021'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'customer.priya@kisansetu.demo', crypt('customer123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Priya Sharma","role":"customer"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000022'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'customer.irfan@kisansetu.demo', crypt('customer123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Mohammed Irfan","role":"customer"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000031'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'logistics.ravi@kisansetu.demo', crypt('logistics123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Ravi Kumar","role":"logistics"}'::jsonb, 'authenticated', 'authenticated', now(), now()),
  ('a0000000-0000-4000-8000-000000000032'::uuid, '00000000-0000-0000-0000-000000000000'::uuid, 'logistics.sandeep@kisansetu.demo', crypt('logistics123', gen_salt('bf')), now(), '{"provider":"email","providers":["email"]}'::jsonb, '{"full_name":"Sandeep Verma","role":"logistics"}'::jsonb, 'authenticated', 'authenticated', now(), now())
) AS v(id, instance_id, email, encrypted_password, email_confirmed_at, raw_app_meta_data, raw_user_meta_data, aud, role, created_at, updated_at)
WHERE NOT EXISTS (SELECT 1 FROM auth.users u WHERE u.id = v.id);

-- ---------- Part 2: sync demo metadata + app records by email ----------
UPDATE auth.users u
SET raw_user_meta_data = u.raw_user_meta_data || v.meta::jsonb
FROM (VALUES
  ('farmer.ramesh@kisansetu.demo',      '{"full_name":"Ramesh Patil","role":"farmer"}'),
  ('farmer.sunita@kisansetu.demo',      '{"full_name":"Sunita Devi","role":"farmer"}'),
  ('farmer.arjun@kisansetu.demo',       '{"full_name":"Arjun Singh","role":"farmer"}'),
  ('farmer.lakshmi@kisansetu.demo',     '{"full_name":"Lakshmi Reddy","role":"farmer"}'),
  ('merchant.kisanagro@kisansetu.demo', '{"full_name":"Kisan Agro Centre","role":"merchant"}'),
  ('merchant.greenfield@kisansetu.demo','{"full_name":"GreenField Seeds","role":"merchant"}'),
  ('merchant.agrocare@kisansetu.demo',  '{"full_name":"AgroCare Supplies","role":"merchant"}'),
  ('customer.priya@kisansetu.demo',     '{"full_name":"Priya Sharma","role":"customer"}'),
  ('customer.irfan@kisansetu.demo',     '{"full_name":"Mohammed Irfan","role":"customer"}'),
  ('logistics.ravi@kisansetu.demo',     '{"full_name":"Ravi Kumar","role":"logistics"}'),
  ('logistics.sandeep@kisansetu.demo',  '{"full_name":"Sandeep Verma","role":"logistics"}')
) AS v(email, meta)
WHERE u.email = v.email
  AND (u.raw_user_meta_data->>'role') IS NULL;

INSERT INTO profiles (user_id, full_name)
SELECT u.id, COALESCE(u.raw_user_meta_data->>'full_name', split_part(u.email, '@', 1))
FROM auth.users u
LEFT JOIN profiles p ON p.user_id = u.id
WHERE p.id IS NULL
  AND u.email IN ('farmer.ramesh@kisansetu.demo', 'farmer.sunita@kisansetu.demo',
                  'farmer.arjun@kisansetu.demo', 'farmer.lakshmi@kisansetu.demo',
                  'merchant.kisanagro@kisansetu.demo', 'merchant.greenfield@kisansetu.demo',
                  'merchant.agrocare@kisansetu.demo', 'customer.priya@kisansetu.demo',
                  'customer.irfan@kisansetu.demo', 'logistics.ravi@kisansetu.demo',
                  'logistics.sandeep@kisansetu.demo');

INSERT INTO user_roles (user_id, role)
SELECT u.id, v.role
FROM (VALUES
  ('farmer.ramesh@kisansetu.demo',      'FARMER'),
  ('farmer.sunita@kisansetu.demo',      'FARMER'),
  ('farmer.arjun@kisansetu.demo',       'FARMER'),
  ('farmer.lakshmi@kisansetu.demo',     'FARMER'),
  ('merchant.kisanagro@kisansetu.demo', 'MERCHANT'),
  ('merchant.greenfield@kisansetu.demo','MERCHANT'),
  ('merchant.agrocare@kisansetu.demo',  'MERCHANT'),
  ('customer.priya@kisansetu.demo',     'CUSTOMER'),
  ('customer.irfan@kisansetu.demo',     'CUSTOMER'),
  ('logistics.ravi@kisansetu.demo',     'LOGISTICS'),
  ('logistics.sandeep@kisansetu.demo',  'LOGISTICS')
) AS v(email, role)
JOIN auth.users u ON u.email = v.email
LEFT JOIN user_roles ur ON ur.user_id = u.id AND ur.role = v.role::app_role
WHERE ur.id IS NULL;

-- Demo account summary:
-- farmer.ramesh@kisansetu.demo     / farmer123
-- farmer.sunita@kisansetu.demo     / farmer123
-- farmer.arjun@kisansetu.demo      / farmer123
-- farmer.lakshmi@kisansetu.demo    / farmer123
-- merchant.kisanagro@kisansetu.demo / merchant123
-- merchant.greenfield@kisansetu.demo / merchant123
-- merchant.agrocare@kisansetu.demo / merchant123
-- customer.priya@kisansetu.demo    / customer123
-- customer.irfan@kisansetu.demo    / customer123
-- logistics.ravi@kisansetu.demo    / logistics123
-- logistics.sandeep@kisansetu.demo / logistics123