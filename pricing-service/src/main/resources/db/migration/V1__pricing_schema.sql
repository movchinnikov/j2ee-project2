-- ============================================================
-- Pricing Service — Schema Migration (V1)
-- Database: pricing_db
-- Schema: pricing
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS pricing;

-- ── Service Types (cleaning service varieties) ────────────────
CREATE TABLE IF NOT EXISTS pricing.service_types (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50) UNIQUE NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    rate_per_sqm     NUMERIC(10,2) NOT NULL DEFAULT 0,
    rate_per_bathroom NUMERIC(10,2) NOT NULL DEFAULT 0,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Service Addons (optional extras) ─────────────────────────
CREATE TABLE IF NOT EXISTS pricing.service_addons (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(50) UNIQUE NOT NULL,
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    price            NUMERIC(10,2) NOT NULL DEFAULT 0,
    duration_minutes INT         NOT NULL DEFAULT 30,
    active           BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ── Seed: Service Types ───────────────────────────────────────
INSERT INTO pricing.service_types (code, name, description, rate_per_sqm, rate_per_bathroom) VALUES
    ('STANDARD',          'Standard Cleaning',         'Regular cleaning of all rooms',            0.50, 15.00),
    ('DEEP',              'Deep Cleaning',              'Thorough cleaning including hard-to-reach', 1.20, 25.00),
    ('POST_CONSTRUCTION', 'Post-Construction Cleaning', 'After renovation dust and debris removal',  1.80, 35.00),
    ('MOVE_IN_OUT',       'Move In/Out Cleaning',      'Full clean for moving situations',          1.50, 30.00)
ON CONFLICT (code) DO NOTHING;

-- ── Seed: Addons ──────────────────────────────────────────────
INSERT INTO pricing.service_addons (code, name, description, price, duration_minutes) VALUES
    ('FRIDGE',   'Fridge Cleaning',    'Interior fridge deep clean',          35.00, 30),
    ('OVEN',     'Oven Cleaning',      'Interior oven degreasing',            40.00, 45),
    ('WINDOWS',  'Window Cleaning',    'Interior window washing',             50.00, 45),
    ('IRONING',  'Ironing Service',    'Ironing a load of clothes',           25.00, 60),
    ('LAUNDRY',  'Laundry Service',    'One load of laundry washed and dried', 30.00, 90),
    ('BALCONY',  'Balcony Cleaning',   'Balcony/patio sweep and wash',        20.00, 30),
    ('CABINETS', 'Cabinet Cleaning',   'Inside all kitchen cabinets',         25.00, 30)
ON CONFLICT (code) DO NOTHING;
