-- ============================================================
-- Booking Service — Schema Migration (V1)
-- Database: booking_db
-- Schema: booking
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS booking;

-- ── Service Types ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking.service_types (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50)   UNIQUE NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    description      TEXT,
    rate_per_sqm     NUMERIC(10,2) NOT NULL DEFAULT 0,
    rate_per_bathroom NUMERIC(10,2) NOT NULL DEFAULT 0,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Service Addons ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking.service_addons (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50)   UNIQUE NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    description      TEXT,
    price            NUMERIC(10,2) NOT NULL DEFAULT 0,
    duration_minutes INT           NOT NULL DEFAULT 30,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Properties ────────────────────────────────────────────────
-- client_id references IAC iac_db.users(id) — no FK (cross-DB)
CREATE TABLE IF NOT EXISTS booking.properties (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID          NOT NULL,
    name            VARCHAR(100),
    type            VARCHAR(30)   NOT NULL,   -- APARTMENT, HOUSE, OFFICE
    area_sqm        NUMERIC(8,2)  NOT NULL,
    bathrooms_count INT           NOT NULL DEFAULT 1,
    address         TEXT,
    notes           TEXT,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Cleaner Profiles ──────────────────────────────────────────
-- user_id references IAC iac_db.users(id) — no FK (cross-DB)
CREATE TABLE IF NOT EXISTS booking.cleaner_profiles (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID          UNIQUE NOT NULL,
    is_available           BOOLEAN       NOT NULL DEFAULT TRUE,
    hourly_rate            NUMERIC(10,2) NOT NULL DEFAULT 25.00,
    rating                 NUMERIC(3,2)  DEFAULT 5.00,
    completed_orders_count INT           NOT NULL DEFAULT 0,
    last_assigned_at       TIMESTAMP,
    bio                    TEXT,
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Orders ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking.orders (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        UUID          NOT NULL,
    property_id      UUID          NOT NULL REFERENCES booking.properties(id),
    service_type_id  UUID          NOT NULL REFERENCES booking.service_types(id),
    cleaner_id       UUID          REFERENCES booking.cleaner_profiles(id),
    frequency        VARCHAR(20)   NOT NULL DEFAULT 'ONE_TIME',
    scheduled_date   TIMESTAMP     NOT NULL,
    duration_hours   NUMERIC(5,2),
    base_price       NUMERIC(10,2) NOT NULL DEFAULT 0,
    discount_percent NUMERIC(5,2)  NOT NULL DEFAULT 0,
    addons_price     NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_price      NUMERIC(10,2) NOT NULL DEFAULT 0,
    status           VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    cancellation_reason TEXT,
    client_notes     TEXT,
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Order Addons (many-to-many) ───────────────────────────────
CREATE TABLE IF NOT EXISTS booking.order_addons (
    order_id       UUID          NOT NULL REFERENCES booking.orders(id) ON DELETE CASCADE,
    addon_id       UUID          NOT NULL REFERENCES booking.service_addons(id),
    price_at_order NUMERIC(10,2) NOT NULL,
    PRIMARY KEY (order_id, addon_id)
);

-- ── Earnings ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking.earnings (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    cleaner_id  UUID          NOT NULL REFERENCES booking.cleaner_profiles(id),
    order_id    UUID          NOT NULL REFERENCES booking.orders(id),
    amount      NUMERIC(10,2) NOT NULL,
    paid_at     TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_bk_properties_client ON booking.properties(client_id);
CREATE INDEX IF NOT EXISTS idx_bk_orders_client     ON booking.orders(client_id);
CREATE INDEX IF NOT EXISTS idx_bk_orders_cleaner    ON booking.orders(cleaner_id);
CREATE INDEX IF NOT EXISTS idx_bk_orders_status     ON booking.orders(status);
CREATE INDEX IF NOT EXISTS idx_bk_orders_date       ON booking.orders(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_bk_earnings_cleaner  ON booking.earnings(cleaner_id);

-- ── Seed: Service Types ───────────────────────────────────────
INSERT INTO booking.service_types (code, name, description, rate_per_sqm, rate_per_bathroom) VALUES
    ('STANDARD',          'Standard Cleaning',         'Regular cleaning of all rooms',            0.50, 15.00),
    ('DEEP',              'Deep Cleaning',              'Thorough cleaning including hard-to-reach', 1.20, 25.00),
    ('POST_CONSTRUCTION', 'Post-Construction Cleaning', 'After renovation dust and debris removal',  1.80, 35.00),
    ('MOVE_IN_OUT',       'Move In/Out Cleaning',      'Full clean for moving situations',          1.50, 30.00)
ON CONFLICT (code) DO NOTHING;

-- ── Seed: Addons ──────────────────────────────────────────────
INSERT INTO booking.service_addons (code, name, description, price, duration_minutes) VALUES
    ('FRIDGE',   'Fridge Cleaning',    'Interior fridge deep clean',          35.00, 30),
    ('OVEN',     'Oven Cleaning',      'Interior oven degreasing',            40.00, 45),
    ('WINDOWS',  'Window Cleaning',    'Interior window washing',             50.00, 45),
    ('IRONING',  'Ironing Service',    'Ironing a load of clothes',           25.00, 60),
    ('LAUNDRY',  'Laundry Service',    'One load of laundry washed and dried', 30.00, 90),
    ('BALCONY',  'Balcony Cleaning',   'Balcony/patio sweep and wash',        20.00, 30),
    ('CABINETS', 'Cabinet Cleaning',   'Inside all kitchen cabinets',         25.00, 30)
ON CONFLICT (code) DO NOTHING;
