-- PROPOSED - NOT APPLIED. Same rule as V5 in this folder: moving this into
-- backend/src/main/resources/db/migration/ is what schedules it to run.
--
-- Finding: farmer_ratings.order_id is UNIQUE and, per the app's own order
-- lifecycle (customer rates a farmer after a customer_orders delivery),
-- clearly intended to reference customer_orders(id) - but V1__init_schema.sql
-- never added that foreign key. Nothing at the database level currently
-- stops a farmer_ratings row from pointing at an order_id that doesn't
-- exist in customer_orders.
--
-- Forward-only, additive, no data loss: run the orphan check at the bottom
-- of docs/database-audit.sql FIRST. If it returns 0, this applies cleanly.
-- If it returns a non-zero count, this migration will fail with a
-- constraint-violation error (safe failure, no partial state) - in that
-- case the orphaned rows need manual remediation (decide per-row: delete
-- the orphan or correct its order_id) before this can be applied, and the
-- query to find them is already in database-audit.sql's bonus section.

ALTER TABLE farmer_ratings
  ADD CONSTRAINT fk_farmer_ratings_order
  FOREIGN KEY (order_id) REFERENCES customer_orders (id)
  ON DELETE CASCADE;
