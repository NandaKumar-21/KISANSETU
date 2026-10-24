-- ============================================================
-- KisanSetu v2 — Development seed data
-- Realistic Indian agricultural data for local development.
-- All user_id values are FIXED so they can be matched 1:1 with
-- auth.users created in Supabase (see db/demo/demo-users.sql).
-- ============================================================

-- ------------------------------------------------------------
-- Profiles (farmer / merchant / customer / logistics)
-- ------------------------------------------------------------
INSERT INTO profiles (id, user_id, full_name, phone, address, city, state, pincode, latitude, longitude) VALUES
 ('a0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000001', 'Ramesh Patil', '+919845000001', 'Ward 4, Patil Farm, Near Taluka Road', 'Kolhapur', 'Maharashtra', '416005', 16.7050, 74.2433),
 ('a0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000002', 'Sunita Devi', '+919876000002', 'Village Bahadarpur, Block Hasanpur', 'Samastipur', 'Bihar', '848101', 25.8600, 85.7900),
 ('a0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000003', 'Arjun Singh', '+919876000003', 'Khalra Road, Amritsar Outskirts', 'Amritsar', 'Punjab', '143001', 31.6340, 74.8723),
 ('a0000000-0000-4000-8000-000000000104', 'a0000000-0000-4000-8000-000000000004', 'Lakshmi Reddy', '+919876000004', 'Bollaram, Near Agricultural Market Yard', 'Hyderabad', 'Telangana', '502325', 17.3850, 78.4867),
 ('a0000000-0000-4000-8000-000000000201', 'a0000000-0000-4000-8000-000000000011', 'Kisan Agro Centre', '+919812000011', 'Main Market Road, Opposite Bus Stand', 'Kolhapur', 'Maharashtra', '416005', 16.7000, 74.2400),
 ('a0000000-0000-4000-8000-000000000202', 'a0000000-0000-4000-8000-000000000012', 'GreenField Seeds & Fertilisers', '+919812000012', 'Station Road, Near Post Office', 'Samastipur', 'Bihar', '848101', 25.8550, 85.7820),
 ('a0000000-0000-4000-8000-000000000203', 'a0000000-0000-4000-8000-000000000013', 'AgroCare Agri Supplies', '+919812000013', 'Shastri Nagar, Market Complex', 'Amritsar', 'Punjab', '143001', 31.6300, 74.8700),
 ('a0000000-0000-4000-8000-000000000301', 'a0000000-0000-4000-8000-000000000021', 'Priya Sharma', '+919843000021', 'Flat 302, Green Heights, Banjara Hills', 'Hyderabad', 'Telangana', '500034', 17.4190, 78.4490),
 ('a0000000-0000-4000-8000-000000000302', 'a0000000-0000-4000-8000-000000000022', 'Mohammed Irfan', '+919843000022', 'House 12, Karol Bagh, New Rohtak Road', 'New Delhi', 'Delhi', '110005', 28.6510, 77.1940),
 ('a0000000-0000-4000-8000-000000000401', 'a0000000-0000-4000-8000-000000000031', 'Ravi Kumar', '+919843000031', 'Room 4, Transport Nagar Hostel', 'Kolhapur', 'Maharashtra', '416005', 16.7020, 74.2380),
 ('a0000000-0000-4000-8000-000000000402', 'a0000000-0000-4000-8000-000000000032', 'Sandeep Verma', '+919843000032', 'Gali No. 8, Patel Nagar', 'Amritsar', 'Punjab', '143001', 31.6320, 74.8740)
ON CONFLICT (user_id) DO NOTHING;

-- ------------------------------------------------------------
-- Roles
-- ------------------------------------------------------------
INSERT INTO user_roles (user_id, role) VALUES
 ('a0000000-0000-4000-8000-000000000001', 'FARMER'),
 ('a0000000-0000-4000-8000-000000000002', 'FARMER'),
 ('a0000000-0000-4000-8000-000000000003', 'FARMER'),
 ('a0000000-0000-4000-8000-000000000004', 'FARMER'),
 ('a0000000-0000-4000-8000-000000000011', 'MERCHANT'),
 ('a0000000-0000-4000-8000-000000000012', 'MERCHANT'),
 ('a0000000-0000-4000-8000-000000000013', 'MERCHANT'),
 ('a0000000-0000-4000-8000-000000000021', 'CUSTOMER'),
 ('a0000000-0000-4000-8000-000000000022', 'CUSTOMER'),
 ('a0000000-0000-4000-8000-000000000031', 'LOGISTICS'),
 ('a0000000-0000-4000-8000-000000000032', 'LOGISTICS')
ON CONFLICT (user_id, role) DO NOTHING;

-- ------------------------------------------------------------
-- Delivery partner availability
-- ------------------------------------------------------------
INSERT INTO delivery_partner_status (partner_id, status) VALUES
 ('a0000000-0000-4000-8000-000000000031', 'AVAILABLE'),
 ('a0000000-0000-4000-8000-000000000032', 'AVAILABLE')
ON CONFLICT (partner_id) DO NOTHING;

-- ------------------------------------------------------------
-- Merchant products
-- ------------------------------------------------------------
INSERT INTO products (id, merchant_id, name, description, category, price, quantity, unit, stock_threshold) VALUES
 ('b0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000011', 'Urea (46-0-0)', 'Nitrogen fertilizer, 50 kg bag', 'Fertilizers', 412.00, 120, 'bag', 15),
 ('b0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000011', 'DAP Fertilizer', 'Di-ammonium phosphate, 50 kg bag', 'Fertilizers', 1350.00, 60, 'bag', 10),
 ('b0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000011', 'Neem Oil (1 L)', 'Organic insecticide concentrate', 'Pesticides', 399.00, 45, 'litre', 10),
 ('b0000000-0000-4000-8000-000000000104', 'a0000000-0000-4000-8000-000000000011', 'Imidacloprid 17.8 SL (250 ml)', 'Systemic insecticide for sucking pests', 'Pesticides', 448.00, 30, 'packet', 8),
 ('b0000000-0000-4000-8000-000000000105', 'a0000000-0000-4000-8000-000000000011', 'Hybrid Tomato Seeds (Hybrid-470)', 'High-yield tomato hybrid, 10 g pack', 'Seeds', 289.00, 80, 'packet', 12),
 ('b0000000-0000-4000-8000-000000000106', 'a0000000-0000-4000-8000-000000000011', 'Manual Spray Pump (16 L)', 'Knapsack sprayer for field application', 'Equipment', 2450.00, 14, 'piece', 3),
 ('b0000000-0000-4000-8000-000000000201', 'a0000000-0000-4000-8000-000000000012', 'Potash (MOP)', 'Muriate of potash, 50 kg bag', 'Fertilizers', 900.00, 90, 'bag', 12),
 ('b0000000-0000-4000-8000-000000000202', 'a0000000-0000-4000-8000-000000000012', 'Mancozeb 75% WP (1 kg)', 'Contact fungicide powder', 'Pesticides', 365.00, 55, 'packet', 10),
 ('b0000000-0000-4000-8000-000000000203', 'a0000000-0000-4000-8000-000000000012', 'Paddy Seeds (BPT-5204)', 'Sona masoori paddy variety, 10 kg', 'Seeds', 620.00, 40, 'packet', 6),
 ('b0000000-0000-4000-8000-000000000204', 'a0000000-0000-4000-8000-000000000012', 'Organic Compost (40 kg)', 'Fully decomposed organic manure', 'Fertilizers', 250.00, 100, 'bag', 15),
 ('b0000000-0000-4000-8000-000000000205', 'a0000000-0000-4000-8000-000000000012', 'Drip Irrigation Kit (¼ acre)', 'Complete drip kit with laterals and emitters', 'Equipment', 3200.00, 8, 'piece', 2),
 ('b0000000-0000-4000-8000-000000000301', 'a0000000-0000-4000-8000-000000000013', 'Wheat Seeds (HD-3086)', 'Disease-resistant wheat variety, 30 kg', 'Seeds', 1050.00, 35, 'packet', 5),
 ('b0000000-0000-4000-8000-000000000302', 'a0000000-0000-4000-8000-000000000013', 'Glyphosate 41% (1 L)', 'Non-selective herbicide', 'Pesticides', 340.00, 70, 'litre', 10),
 ('b0000000-0000-4000-8000-000000000303', 'a0000000-0000-4000-8000-000000000013', 'NPK 19:19:19 (25 kg)', 'Water soluble balanced fertilizer', 'Fertilizers', 780.00, 50, 'bag', 8),
 ('b0000000-0000-4000-8000-000000000304', 'a0000000-0000-4000-8000-000000000013', 'Cattle Feed (50 kg)', 'Balanced livestock nutrition feed', 'Feed', 890.00, 25, 'bag', 5),
 ('b0000000-0000-4000-8000-000000000305', 'a0000000-0000-4000-8000-000000000013', 'Garden Hoe (Hand)', 'Durable iron hand hoe for weeding', 'Equipment', 320.00, 18, 'piece', 4)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- Farmer marketplace listings
-- ------------------------------------------------------------
INSERT INTO marketplace_listings (id, farmer_id, title, description, category, price, quantity, unit, location, status, variety, farming_method, harvest_date, latitude, longitude) VALUES
 ('c0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000001', 'Fresh Tomatoes', 'Farm-fresh hybrid tomatoes, graded and cleaned. Ideal for retail and hotels.', 'Vegetables', 35.00, 400, 'kg', 'Kolhapur, Maharashtra', 'ACTIVE', 'Hybrid-470', 'conventional', DATE '2026-08-20', 16.7050, 74.2433),
 ('c0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000001', 'Green Chillies', 'Pungent green chillies harvested this morning.', 'Spices', 60.00, 150, 'kg', 'Kolhapur, Maharashtra', 'ACTIVE', 'Byadgi', 'conventional', DATE '2026-08-18', 16.7050, 74.2433),
 ('c0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000001', 'Onions (Red)', 'Red onions from Rabi crop, well-cured and stored.', 'Vegetables', 28.00, 800, 'kg', 'Kolhapur, Maharashtra', 'ACTIVE', 'Nasik Red', 'conventional', DATE '2026-09-05', 16.7050, 74.2433),
 ('c0000000-0000-4000-8000-000000000104', 'a0000000-0000-4000-8000-000000000001', 'Banana (Robusta)', 'Ripening-stage Robusta bananas, 6 hands per bunch.', 'Fruits', 30.00, 200, 'dozen', 'Kolhapur, Maharashtra', 'ACTIVE', 'Robusta', 'conventional', DATE '2026-08-25', 16.7050, 74.2433),
 ('c0000000-0000-4000-8000-000000000201', 'a0000000-0000-4000-8000-000000000002', 'Paddy (Sona Masoori)', 'Freshly harvested Sona Masoori paddy, moisture 14%.', 'Grains', 24.00, 1500, 'kg', 'Samastipur, Bihar', 'ACTIVE', 'BPT-5204', 'conventional', DATE '2026-08-30', 25.8600, 85.7900),
 ('c0000000-0000-4000-8000-000000000202', 'a0000000-0000-4000-8000-000000000002', 'Organic Mango (Langra)', 'Chemical-free Langra mangoes from village orchard.', 'Fruits', 85.00, 120, 'kg', 'Samastipur, Bihar', 'ACTIVE', 'Langra', 'organic', DATE '2026-08-22', 25.8600, 85.7900),
 ('c0000000-0000-4000-8000-000000000203', 'a0000000-0000-4000-8000-000000000002', 'Wheat (HD-3086)', 'Premium quality wheat, sun-dried and cleaned.', 'Grains', 22.00, 2000, 'kg', 'Samastipur, Bihar', 'ACTIVE', 'HD-3086', 'conventional', DATE '2026-09-10', 25.8600, 85.7900),
 ('c0000000-0000-4000-8000-000000000301', 'a0000000-0000-4000-8000-000000000003', 'Basmati Rice', 'Long-grain aromatic basmati, single polish.', 'Grains', 72.00, 500, 'kg', 'Amritsar, Punjab', 'ACTIVE', 'Pusa 1121', 'conventional', DATE '2026-09-15', 31.6340, 74.8723),
 ('c0000000-0000-4000-8000-000000000302', 'a0000000-0000-4000-8000-000000000003', 'Potatoes', 'Grade-A potatoes, suitable for all cooking.', 'Vegetables', 24.00, 600, 'kg', 'Amritsar, Punjab', 'ACTIVE', 'Kufri Jyoti', 'conventional', DATE '2026-08-19', 31.6340, 74.8723),
 ('c0000000-0000-4000-8000-000000000303', 'a0000000-0000-4000-8000-000000000003', 'Yellow Maize', 'Feed-grade yellow maize, dried and shelled.', 'Grains', 20.00, 1000, 'kg', 'Amritsar, Punjab', 'ACTIVE', 'Pioneer 3377', 'conventional', DATE '2026-08-28', 31.6340, 74.8723),
 ('c0000000-0000-4000-8000-000000000401', 'a0000000-0000-4000-8000-000000000004', 'Groundnut (Bold)', 'Hand-shelled bold groundnut with high oil content.', 'Pulses', 68.00, 300, 'kg', 'Hyderabad, Telangana', 'ACTIVE', 'TAG-24', 'conventional', DATE '2026-09-01', 17.3850, 78.4867),
 ('c0000000-0000-4000-8000-000000000402', 'a0000000-0000-4000-8000-000000000004', 'Red Chilli Powder', 'Home-ground spicy chilli powder, no additives.', 'Spices', 160.00, 80, 'kg', 'Hyderabad, Telangana', 'ACTIVE', 'Guntur', 'organic', DATE '2026-08-21', 17.3850, 78.4867),
 ('c0000000-0000-4000-8000-000000000403', 'a0000000-0000-4000-8000-000000000004', 'Toor Dal (Arhar)', 'Unpolished toor dal, machine cleaned.', 'Pulses', 120.00, 250, 'kg', 'Hyderabad, Telangana', 'ACTIVE', 'Local Desi', 'conventional', DATE '2026-09-08', 17.3850, 78.4867)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- Farmer -> Merchant orders (inputs purchased by farmers)
-- ------------------------------------------------------------
INSERT INTO orders (id, farmer_id, merchant_id, product_id, quantity, unit_price, total_price, status, notes, created_at) VALUES
 ('d0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000101', 3, 412.00, 1236.00, 'PENDING', 'Need before Thursday', now() - interval '1 day'),
 ('d0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000202', 2, 365.00, 730.00, 'ACCEPTED', NULL, now() - interval '2 days'),
 ('d0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000013', 'b0000000-0000-4000-8000-000000000303', 2, 780.00, 1560.00, 'COMPLETED', NULL, now() - interval '6 days'),
 ('d0000000-0000-4000-8000-000000000104', 'a0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000011', 'b0000000-0000-4000-8000-000000000103', 4, 399.00, 1596.00, 'REJECTED', 'Currently out of stock', now() - interval '4 days'),
 ('d0000000-0000-4000-8000-000000000105', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000012', 'b0000000-0000-4000-8000-000000000201', 1, 900.00, 900.00, 'ACCEPTED', NULL, now() - interval '3 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory_transactions (product_id, change_qty, reason, order_id, created_by, created_at) VALUES
 ('b0000000-0000-4000-8000-000000000202', -2, 'order_accept', 'd0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000012', now() - interval '2 days'),
 ('b0000000-0000-4000-8000-000000000303', -2, 'order_accept', 'd0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000013', now() - interval '6 days'),
 ('b0000000-0000-4000-8000-000000000201', -1, 'order_accept', 'd0000000-0000-4000-8000-000000000105', 'a0000000-0000-4000-8000-000000000012', now() - interval '3 hours');

-- ------------------------------------------------------------
-- Customer addresses
-- ------------------------------------------------------------
INSERT INTO customer_addresses (id, customer_id, label, address_line, city, state, pincode, phone, latitude, longitude, is_default) VALUES
 ('e0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000021', 'Home', 'Flat 302, Green Heights, Banjara Hills', 'Hyderabad', 'Telangana', '500034', '+919843000021', 17.4190, 78.4490, true),
 ('e0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000021', 'Office', 'Tower B, Hitech City Main Road', 'Hyderabad', 'Telangana', '500081', '+919843000021', 17.4435, 78.3772, false),
 ('e0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000022', 'Home', 'House 12, Karol Bagh, New Rohtak Road', 'New Delhi', 'Delhi', '110005', '+919843000022', 28.6510, 77.1940, true)
ON CONFLICT (id) DO NOTHING;

-- ------------------------------------------------------------
-- Customer orders + delivery assignments
-- ------------------------------------------------------------
INSERT INTO customer_orders (id, customer_id, farmer_id, listing_id, quantity, unit_price, total_price, status, delivery_address_id, delivery_preference, estimated_delivery, notes, farmer_notes, delivery_partner_id, delivery_status, delivered_at, created_at) VALUES
 ('f0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000021', 'a0000000-0000-4000-8000-000000000004', 'c0000000-0000-4000-8000-000000000401', 10, 68.00, 680.00, 'DELIVERED', 'e0000000-0000-4000-8000-000000000101', 'morning', now() + interval '2 days', 'Please deliver before 10 AM', 'Packed fresh', 'a0000000-0000-4000-8000-000000000031', 'COMPLETED', now() - interval '1 day', now() - interval '5 days'),
 ('f0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000021', 'a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000101', 25, 35.00, 875.00, 'DELIVERED', 'e0000000-0000-4000-8000-000000000101', 'any', now() + interval '2 days', NULL, NULL, 'a0000000-0000-4000-8000-000000000031', 'COMPLETED', now() - interval '4 days', now() - interval '9 days'),
 ('f0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000021', 'a0000000-0000-4000-8000-000000000003', 'c0000000-0000-4000-8000-000000000302', 40, 24.00, 960.00, 'CONFIRMED', 'e0000000-0000-4000-8000-000000000101', 'evening', now() + interval '3 days', 'Extra large potatoes please', NULL, 'a0000000-0000-4000-8000-000000000032', 'ASSIGNED', NULL, now() - interval '8 hours'),
 ('f0000000-0000-4000-8000-000000000104', 'a0000000-0000-4000-8000-000000000022', 'a0000000-0000-4000-8000-000000000002', 'c0000000-0000-4000-8000-000000000202', 5, 85.00, 425.00, 'PENDING', 'e0000000-0000-4000-8000-000000000103', 'any', now() + interval '3 days', NULL, NULL, NULL, 'PENDING_ASSIGNMENT', NULL, now() - interval '2 hours'),
 ('f0000000-0000-4000-8000-000000000105', 'a0000000-0000-4000-8000-000000000022', 'a0000000-0000-4000-8000-000000000001', 'c0000000-0000-4000-8000-000000000103', 12, 28.00, 336.00, 'CANCELLED', 'e0000000-0000-4000-8000-000000000103', 'morning', now() + interval '3 days', 'Cancelled - ordering later', NULL, NULL, NULL, NULL, now() - interval '3 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO deliveries (id, order_id, partner_id, status, route_sequence, distance_km, earning, accepted_at, picked_up_at, in_transit_at, delivered_at) VALUES
 ('f1000000-0000-4000-8000-000000000001', 'f0000000-0000-4000-8000-000000000101', 'a0000000-0000-4000-8000-000000000031', 'COMPLETED', 1, 12.4, 34.00, now() - interval '3 days', now() - interval '2 days', now() - interval '2 days', now() - interval '1 day'),
 ('f1000000-0000-4000-8000-000000000002', 'f0000000-0000-4000-8000-000000000102', 'a0000000-0000-4000-8000-000000000031', 'COMPLETED', 2, 15.1, 43.75, now() - interval '6 days', now() - interval '5 days', now() - interval '5 days', now() - interval '4 days'),
 ('f1000000-0000-4000-8000-000000000003', 'f0000000-0000-4000-8000-000000000103', 'a0000000-0000-4000-8000-000000000032', 'ASSIGNED', 1, 8.2, NULL, NULL, NULL, NULL, NULL);

-- ------------------------------------------------------------
-- Order status history
-- ------------------------------------------------------------
INSERT INTO order_status_history (order_type, order_id, from_status, to_status, changed_by, created_at) VALUES
 ('customer', 'f0000000-0000-4000-8000-000000000101', NULL, 'pending', 'a0000000-0000-4000-8000-000000000021', now() - interval '5 days'),
 ('customer', 'f0000000-0000-4000-8000-000000000101', 'pending', 'confirmed', 'a0000000-0000-4000-8000-000000000004', now() - interval '4 days'),
 ('customer', 'f0000000-0000-4000-8000-000000000101', 'confirmed', 'packed', 'a0000000-0000-4000-8000-000000000004', now() - interval '4 days'),
 ('customer', 'f0000000-0000-4000-8000-000000000101', 'packed', 'dispatched', 'a0000000-0000-4000-8000-000000000004', now() - interval '3 days'),
 ('customer', 'f0000000-0000-4000-8000-000000000101', 'dispatched', 'delivered', 'a0000000-0000-4000-8000-000000000031', now() - interval '1 day'),
 ('merchant', 'd0000000-0000-4000-8000-000000000102', NULL, 'pending', 'a0000000-0000-4000-8000-000000000002', now() - interval '2 days'),
 ('merchant', 'd0000000-0000-4000-8000-000000000102', 'pending', 'accepted', 'a0000000-0000-4000-8000-000000000012', now() - interval '2 days');

-- ------------------------------------------------------------
-- Farmer calendar events
-- ------------------------------------------------------------
INSERT INTO farmer_calendar (id, farmer_id, title, description, event_type, event_date, reminder_enabled, completed, crop_type, weather_dependent, suggested_by_ai) VALUES
 ('f2000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'Tomato transplanting', 'Transplant hybrid seedlings to main field', 'planting', CURRENT_DATE + 2, true, false, 'Tomato', true, false),
 ('f2000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'Irrigation - Onion field', 'Morning drip irrigation cycle', 'irrigation', CURRENT_DATE, true, false, 'Onion', true, false),
 ('f2000000-0000-4000-8000-000000000003', 'a0000000-0000-4000-8000-000000000001', 'Fertilizer application', 'NPK 19:19:19 foliar spray on chillies', 'fertilizing', CURRENT_DATE + 5, true, false, 'Chilli', false, true),
 ('f2000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000002', 'Harvest paddy', 'Morning harvest before 11 AM', 'harvest', CURRENT_DATE + 7, true, false, 'Paddy', true, false);

-- ------------------------------------------------------------
-- Farmer ratings
-- ------------------------------------------------------------
INSERT INTO farmer_ratings (id, farmer_id, customer_id, order_id, rating, review, created_at) VALUES
 ('f3000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000004', 'a0000000-0000-4000-8000-000000000021', 'f0000000-0000-4000-8000-000000000101', 5, 'Very fresh groundnuts, excellent quality!', now() - interval '1 day'),
 ('f3000000-0000-4000-8000-000000000002', 'a0000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000021', 'f0000000-0000-4000-8000-000000000102', 4, 'Good tomatoes, delivery took a bit long.', now() - interval '4 days');

-- ------------------------------------------------------------
-- Notifications
-- ------------------------------------------------------------
INSERT INTO notifications (user_id, type, title, message, data, read, created_at) VALUES
 ('a0000000-0000-4000-8000-000000000001', 'order_received', 'New merchant order', 'Farmer Ramesh Patil ordered Urea (46-0-0) x3', '{"order_id":"d0000000-0000-4000-8000-000000000101"}'::jsonb, false, now() - interval '1 day'),
 ('a0000000-0000-4000-8000-000000000001', 'order_accepted', 'Order accepted', 'Kisan Agro Centre accepted your order #D000...0101', NULL, true, now() - interval '20 hours'),
 ('a0000000-0000-4000-8000-000000000004', 'marketplace_order', 'New customer order', 'New customer order for Groundnut (Bold) x10 kg', '{"order_id":"f0000000-0000-4000-8000-000000000101"}'::jsonb, false, now() - interval '5 days'),
 ('a0000000-0000-4000-8000-000000000021', 'order_status', 'Order delivered', 'Your Groundnut (Bold) order was delivered', NULL, false, now() - interval '1 day'),
 ('a0000000-0000-4000-8000-000000000031', 'delivery_assigned', 'New delivery assigned', 'Delivery assigned for order F000...0103', NULL, false, now() - interval '8 hours');

-- ------------------------------------------------------------
-- Customer preferences (recommendation cache)
-- ------------------------------------------------------------
INSERT INTO customer_preferences (customer_id, preferred_categories, preferred_farmers, last_recommendations, recommendations_updated_at) VALUES
 ('a0000000-0000-4000-8000-000000000021', '{Vegetables,Pulses}', '{a0000000-0000-4000-8000-000000000001,a0000000-0000-4000-8000-000000000004}', '{"recommendations":[{"title":"Fresh Tomatoes","category":"Vegetables","reason":"You bought fresh produce from this farmer before","priority":"high"},{"title":"Toor Dal (Arhar)","category":"Pulses","reason":"Matches your preferred pulses category","priority":"medium"}],"seasonal_tip":"August is peak season for kharif vegetables - buy fresh tomatoes and chillies."}'::jsonb, now() - interval '30 minutes');

-- ------------------------------------------------------------
-- Crop plans history
-- ------------------------------------------------------------
INSERT INTO crop_plans (farmer_id, input_json, result_json, created_at) VALUES
 ('a0000000-0000-4000-8000-000000000001',
  '{"soilType":"loamy","region":"Kolhapur, Maharashtra","season":"Kharif","waterAvailability":"Medium","budget":50000,"farmSize":2}'::jsonb,
  '{"recommendations":[{"crop":"Tomato","expectedProfit":"₹32,000 / acre","expectedProfitValue":32000,"expectedPriceRange":"₹14-18/kg","fertilizers":["NPK 19:19:19","Organic compost"],"waterNeed":"Medium","whyRecommended":"Short-season cash crop ideal for loamy soil"}]}'::jsonb,
  now() - interval '3 days');

-- ------------------------------------------------------------
-- AI conversations
-- ------------------------------------------------------------
INSERT INTO ai_conversations (id, user_id, title, created_at, updated_at) VALUES
 ('f4000000-0000-4000-8000-000000000001', 'a0000000-0000-4000-8000-000000000001', 'Tomato disease help', now() - interval '2 days', now() - interval '2 days');

INSERT INTO ai_messages (conversation_id, role, content, created_at) VALUES
 ('f4000000-0000-4000-8000-000000000001', 'user', 'My tomato leaves have yellow spots, what should I do?', now() - interval '2 days'),
 ('f4000000-0000-4000-8000-000000000001', 'assistant', 'Yellow spots on tomato leaves often indicate early blight or Septoria leaf spot. Remove affected leaves, improve air circulation, and apply a copper-based fungicide early in the morning. Keep the field weed-free and avoid overhead irrigation in the evening.', now() - interval '2 days');

-- ------------------------------------------------------------
-- Disease records
-- ------------------------------------------------------------
INSERT INTO disease_records (farmer_id, detection_method, symptoms, disease_name, confidence, severity, description, created_at) VALUES
 ('a0000000-0000-4000-8000-000000000001', 'symptom', '{yellow_leaves,brown_spots}', 'Early Blight (Alternaria solani)', 'medium', 'medium', 'Fungal disease common on tomato leaves in humid conditions.', now() - interval '6 days');
