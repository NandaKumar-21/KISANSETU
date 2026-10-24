-- ============================================================
-- KisanSetu v2 — Core schema (port of the original Supabase schema)
-- Run by Spring Boot Flyway against Supabase PostgreSQL.
-- The Spring Boot backend is the only application talking to this
-- database; RLS policies from the original Supabase-only app are
-- therefore intentionally omitted (authorization is enforced in
-- Spring Security + service layer ownership checks instead).
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ------------------------------------------------------------
-- Enums
-- ------------------------------------------------------------
DO $$
BEGIN
    CREATE TYPE app_role AS ENUM ('FARMER', 'MERCHANT', 'CUSTOMER', 'LOGISTICS');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE listing_status AS ENUM ('ACTIVE', 'SOLD', 'EXPIRED', 'PAUSED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE customer_order_status AS ENUM ('PENDING', 'CONFIRMED', 'PACKED', 'DISPATCHED', 'DELIVERED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE delivery_status AS ENUM ('PENDING_ASSIGNMENT', 'ASSIGNED', 'ACCEPTED', 'PICKUP_SCHEDULED', 'PICKED_UP', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED', 'REJECTED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE merchant_order_status AS ENUM ('PENDING', 'ACCEPTED', 'PROCESSING', 'COMPLETED', 'REJECTED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE marketplace_order_status AS ENUM ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TYPE partner_availability AS ENUM ('AVAILABLE', 'BUSY', 'OFFLINE');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- ------------------------------------------------------------
-- users / profiles / roles
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profiles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL UNIQUE,
    full_name     TEXT NOT NULL,
    phone         TEXT,
    address       TEXT,
    city          TEXT,
    state         TEXT,
    pincode       TEXT,
    avatar_url    TEXT,
    latitude      DECIMAL(10, 8),
    longitude     DECIMAL(11, 8),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_roles (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    role       app_role NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, role)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles (user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_user ON profiles (user_id);

-- ------------------------------------------------------------
-- Merchant products (agricultural inputs)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    merchant_id UUID NOT NULL,
    name        TEXT NOT NULL,
    description TEXT,
    category    TEXT,
    price       DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    quantity    INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    unit        TEXT NOT NULL DEFAULT 'kg',
    image_url   TEXT,
    stock_threshold INTEGER NOT NULL DEFAULT 10,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_products_merchant ON products (merchant_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);

-- ------------------------------------------------------------
-- Farmer produce listings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS marketplace_listings (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id      UUID NOT NULL,
    title          TEXT NOT NULL,
    description    TEXT,
    category       TEXT NOT NULL,
    price          NUMERIC NOT NULL CHECK (price > 0),
    quantity       NUMERIC NOT NULL CHECK (quantity >= 0),
    unit           TEXT NOT NULL DEFAULT 'kg',
    image_url      TEXT,
    location       TEXT,
    status         listing_status NOT NULL DEFAULT 'ACTIVE',
    variety        TEXT,
    farming_method TEXT DEFAULT 'conventional',
    harvest_date   DATE,
    latitude       DECIMAL(10, 8),
    longitude      DECIMAL(11, 8),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listings_farmer ON marketplace_listings (farmer_id);
CREATE INDEX IF NOT EXISTS idx_listings_status ON marketplace_listings (status);
CREATE INDEX IF NOT EXISTS idx_listings_category ON marketplace_listings (category);
CREATE INDEX IF NOT EXISTS idx_listings_title_lower ON marketplace_listings (lower(title));

-- ------------------------------------------------------------
-- Farmer -> Merchant orders (agricultural input purchases)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id   UUID NOT NULL,
    merchant_id UUID NOT NULL,
    product_id  UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity    INTEGER NOT NULL CHECK (quantity > 0),
    unit_price  DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status      merchant_order_status NOT NULL DEFAULT 'PENDING',
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_orders_farmer ON orders (farmer_id);
CREATE INDEX IF NOT EXISTS idx_orders_merchant ON orders (merchant_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);

-- ------------------------------------------------------------
-- Farmer -> Farmer marketplace orders
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS marketplace_orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id  UUID NOT NULL REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    buyer_id    UUID NOT NULL,
    farmer_id   UUID NOT NULL,
    quantity    NUMERIC NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC NOT NULL,
    total_price NUMERIC NOT NULL,
    status      marketplace_order_status NOT NULL DEFAULT 'PENDING',
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_orders_buyer ON marketplace_orders (buyer_id);
CREATE INDEX IF NOT EXISTS idx_marketplace_orders_farmer ON marketplace_orders (farmer_id);

-- ------------------------------------------------------------
-- Customer addresses / cart / wishlist / orders
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_addresses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   UUID NOT NULL,
    label         TEXT NOT NULL DEFAULT 'Home',
    address_line  TEXT NOT NULL,
    city          TEXT NOT NULL,
    state         TEXT NOT NULL,
    pincode       TEXT NOT NULL,
    phone         TEXT,
    latitude      DECIMAL(10, 8),
    longitude     DECIMAL(11, 8),
    is_default    BOOLEAN DEFAULT false,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_addresses_customer ON customer_addresses (customer_id);

CREATE TABLE IF NOT EXISTS customer_cart (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    listing_id  UUID NOT NULL REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    quantity    NUMERIC NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (customer_id, listing_id)
);

CREATE INDEX IF NOT EXISTS idx_cart_customer ON customer_cart (customer_id);

CREATE TABLE IF NOT EXISTS customer_wishlist (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    listing_id  UUID REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    farmer_id   UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (customer_id, listing_id),
    UNIQUE (customer_id, farmer_id)
);

CREATE INDEX IF NOT EXISTS idx_wishlist_customer ON customer_wishlist (customer_id);

CREATE TABLE IF NOT EXISTS customer_orders (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id           UUID NOT NULL,
    farmer_id             UUID NOT NULL,
    listing_id            UUID NOT NULL REFERENCES marketplace_listings (id),
    quantity              NUMERIC NOT NULL CHECK (quantity > 0),
    unit_price            NUMERIC NOT NULL,
    total_price           NUMERIC NOT NULL,
    status                customer_order_status NOT NULL DEFAULT 'PENDING',
    delivery_address_id   UUID REFERENCES customer_addresses (id),
    delivery_preference   TEXT DEFAULT 'any',
    estimated_delivery    TIMESTAMPTZ,
    notes                 TEXT,
    farmer_notes          TEXT,
    farmer_contact_visible BOOLEAN DEFAULT false,
    contact_disabled_at   TIMESTAMPTZ,
    packed_at             TIMESTAMPTZ,
    dispatched_at         TIMESTAMPTZ,
    delivered_at          TIMESTAMPTZ,
    delivery_partner_id   UUID,
    delivery_status       delivery_status,
    pickup_time           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_customer_orders_customer ON customer_orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_orders_farmer ON customer_orders (farmer_id);
CREATE INDEX IF NOT EXISTS idx_customer_orders_partner ON customer_orders (delivery_partner_id);
CREATE INDEX IF NOT EXISTS idx_customer_orders_status ON customer_orders (status);

-- ------------------------------------------------------------
-- Order status history (audit)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_type  TEXT NOT NULL CHECK (order_type IN ('customer', 'merchant', 'marketplace', 'delivery')),
    order_id    UUID NOT NULL,
    from_status TEXT,
    to_status   TEXT NOT NULL,
    changed_by  UUID,
    note        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_status_history_order ON order_status_history (order_type, order_id);

-- ------------------------------------------------------------
-- Inventory transactions (audit)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id   UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    change_qty   INTEGER NOT NULL,
    reason       TEXT NOT NULL,
    order_id     UUID,
    created_by   UUID,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_product ON inventory_transactions (product_id);

-- ------------------------------------------------------------
-- Farmer disease detection records
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS disease_records (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id                 UUID NOT NULL,
    detection_method          TEXT NOT NULL CHECK (detection_method IN ('image', 'symptom')),
    image_url                 TEXT,
    symptoms                  TEXT[],
    disease_name              TEXT,
    confidence                TEXT CHECK (confidence IN ('high', 'medium', 'low')),
    severity                  TEXT,
    description               TEXT,
    analysis_json             JSONB,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_disease_records_farmer ON disease_records (farmer_id);

-- ------------------------------------------------------------
-- Farmer calendar
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS farmer_calendar (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id          UUID NOT NULL,
    title              TEXT NOT NULL,
    description        TEXT,
    event_type         TEXT NOT NULL,
    event_date         DATE NOT NULL,
    reminder_enabled   BOOLEAN DEFAULT true,
    completed          BOOLEAN DEFAULT false,
    crop_type          TEXT,
    weather_dependent  BOOLEAN DEFAULT false,
    suggested_by_ai    BOOLEAN DEFAULT false,
    notification_sent  BOOLEAN DEFAULT false,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_calendar_farmer ON farmer_calendar (farmer_id, event_date);

-- ------------------------------------------------------------
-- Farmer ratings
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS farmer_ratings (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id   UUID NOT NULL,
    customer_id UUID NOT NULL,
    order_id    UUID NOT NULL,
    rating      INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (order_id)
);

CREATE INDEX IF NOT EXISTS idx_ratings_farmer ON farmer_ratings (farmer_id);

-- ------------------------------------------------------------
-- Notifications
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    type       TEXT NOT NULL,
    title      TEXT NOT NULL,
    message    TEXT NOT NULL,
    data       JSONB,
    read       BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications (user_id, created_at DESC);

-- ------------------------------------------------------------
-- Customer preferences + availability alerts (recommendations)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customer_preferences (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id               UUID NOT NULL UNIQUE,
    preferred_categories      TEXT[] DEFAULT '{}',
    preferred_farmers         UUID[] DEFAULT '{}',
    last_recommendations      JSONB,
    recommendations_updated_at TIMESTAMPTZ,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS availability_alerts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL,
    listing_id   UUID REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    category     TEXT,
    farmer_id    UUID,
    is_active    BOOLEAN DEFAULT true,
    triggered_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_alerts_customer ON availability_alerts (customer_id);

-- ------------------------------------------------------------
-- Order invoices
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_invoices (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    invoice_number TEXT NOT NULL,
    generated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    html_content   TEXT,
    UNIQUE (order_id)
);

-- ------------------------------------------------------------
-- Logistics: partner availability + deliveries
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS delivery_partner_status (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    partner_id      UUID NOT NULL UNIQUE,
    status          partner_availability NOT NULL DEFAULT 'AVAILABLE',
    last_assigned_at TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS deliveries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID NOT NULL UNIQUE REFERENCES customer_orders (id) ON DELETE CASCADE,
    partner_id     UUID,
    status         delivery_status,
    route_sequence INTEGER,
    distance_km    NUMERIC,
    earning        NUMERIC,
    accepted_at    TIMESTAMPTZ,
    picked_up_at   TIMESTAMPTZ,
    in_transit_at  TIMESTAMPTZ,
    delivered_at   TIMESTAMPTZ,
    failed_at      TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_deliveries_partner ON deliveries (partner_id, status);

-- ------------------------------------------------------------
-- Weather cache (short TTL)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS weather_cache (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    latitude      DECIMAL(10, 8) NOT NULL,
    longitude     DECIMAL(11, 8) NOT NULL,
    payload       JSONB NOT NULL,
    cached_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (latitude, longitude)
);

-- ------------------------------------------------------------
-- AI conversations / messages (chatbot persistence)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_conversations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL,
    title      TEXT NOT NULL DEFAULT 'New conversation',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_conversations_user ON ai_conversations (user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations (id) ON DELETE CASCADE,
    role            TEXT NOT NULL CHECK (role IN ('user', 'assistant')),
    content         TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON ai_messages (conversation_id, created_at);

-- ------------------------------------------------------------
-- Crop planner history
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS crop_plans (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    farmer_id  UUID NOT NULL,
    input_json JSONB NOT NULL,
    result_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_crop_plans_farmer ON crop_plans (farmer_id);

-- ------------------------------------------------------------
-- Timestamp maintenance
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['profiles', 'products', 'marketplace_listings', 'orders', 'marketplace_orders',
                            'customer_addresses', 'customer_cart', 'customer_orders', 'farmer_calendar',
                            'customer_preferences', 'delivery_partner_status', 'deliveries', 'ai_conversations']
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'update_' || t || '_updated_at') THEN
            EXECUTE format('CREATE TRIGGER update_%I_updated_at BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()', t, t);
        END IF;
    END LOOP;
END $$;