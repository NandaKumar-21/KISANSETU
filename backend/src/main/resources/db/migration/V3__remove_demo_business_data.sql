-- ============================================================
-- KisanSetu v2 — Remove demo business data
-- 
-- This migration removes all pre-populated demo business data from V2__seed_data.sql
-- while preserving required authentication/reference data for application functionality.
--
-- Business tables will start empty after this migration.
-- Only user authentication and role data is preserved for development/testing.
-- ============================================================

-- ------------------------------------------------------------
-- Remove AI conversation data (business data)
-- Order: Remove child records first (ai_messages), then parent (ai_conversations)
-- ------------------------------------------------------------
DELETE FROM ai_messages WHERE conversation_id IN (
    SELECT id FROM ai_conversations WHERE user_id::text LIKE 'a0000000-0000-4000-8000-%'
);
DELETE FROM ai_conversations WHERE user_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove disease detection records (business data)
-- ------------------------------------------------------------
DELETE FROM disease_records WHERE farmer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove crop planning history (business data)
-- ------------------------------------------------------------
DELETE FROM crop_plans WHERE farmer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove customer preferences/recommendations (business data)
-- ------------------------------------------------------------
DELETE FROM customer_preferences WHERE customer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove availability alerts (business data)
-- ------------------------------------------------------------
DELETE FROM availability_alerts WHERE customer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove farmer ratings (business data)
-- ------------------------------------------------------------
DELETE FROM farmer_ratings WHERE farmer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove farmer calendar events (business data)
-- ------------------------------------------------------------
DELETE FROM farmer_calendar WHERE farmer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove notifications (business data)
-- ------------------------------------------------------------
DELETE FROM notifications WHERE user_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove order status history (business data)
-- ------------------------------------------------------------
DELETE FROM order_status_history WHERE order_id::text LIKE 'f0000000-0000-4000-8000-%'
   OR order_id::text LIKE 'd0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove delivery records (business data)
-- Order: Deliveries reference customer_orders
-- ------------------------------------------------------------
DELETE FROM deliveries WHERE order_id::text LIKE 'f0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove order invoices (business data - not seeded in V2 but cleanup if any)
-- Order: Invoices reference customer_orders
-- ------------------------------------------------------------
DELETE FROM order_invoices WHERE order_id::text LIKE 'f0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove customer orders (business data)
-- Order: Customer orders reference marketplace_listings, customer_addresses
-- ------------------------------------------------------------
DELETE FROM customer_orders WHERE id::text LIKE 'f0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove customer addresses (business data)
-- ------------------------------------------------------------
DELETE FROM customer_addresses WHERE id::text LIKE 'e0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove inventory transactions (business data)
-- Order: Inventory transactions reference products and orders
-- ------------------------------------------------------------
DELETE FROM inventory_transactions WHERE order_id::text LIKE 'd0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove farmer -> merchant orders (business data)
-- Order: Orders reference products
-- ------------------------------------------------------------
DELETE FROM orders WHERE id::text LIKE 'd0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove farmer -> farmer marketplace orders (business data)
-- Order: Marketplace orders reference marketplace_listings
-- Note: marketplace_orders not seeded in V2, but cleanup if any exist
-- ------------------------------------------------------------
DELETE FROM marketplace_orders WHERE id::text LIKE 'c0000000-0000-4000-8000-%' 
   OR id::text LIKE 'd0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove customer cart items (business data - not seeded in V2 but cleanup if any)
-- Order: Cart items reference marketplace_listings
-- ------------------------------------------------------------
DELETE FROM customer_cart WHERE customer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove customer wishlist items (business data - not seeded in V2 but cleanup if any)
-- Order: Wishlist items reference marketplace_listings and farmers
-- ------------------------------------------------------------
DELETE FROM customer_wishlist WHERE customer_id::text LIKE 'a0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove farmer marketplace listings (business data)
-- Order: Listings are referenced by marketplace_orders, customer_orders, customer_cart, customer_wishlist
-- ------------------------------------------------------------
DELETE FROM marketplace_listings WHERE id::text LIKE 'c0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Remove merchant products (business data)
-- Order: Products are referenced by orders and inventory_transactions
-- ------------------------------------------------------------
DELETE FROM products WHERE id::text LIKE 'b0000000-0000-4000-8000-%';

-- ------------------------------------------------------------
-- Note: Authentication data preserved
-- ------------------------------------------------------------
-- The following tables are NOT cleaned as they contain required 
-- authentication and reference data for application functionality:
-- - profiles (user profile data for authentication)
-- - user_roles (role assignments for authorization) 
-- - delivery_partner_status (logistics partner availability)
-- 
-- These can be used for development/testing but contain no business content.
-- ============================================================