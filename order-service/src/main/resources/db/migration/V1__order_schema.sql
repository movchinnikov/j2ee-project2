-- ============================================================
-- Order Service — Schema Migration (V1)
-- Database: order_db
-- Schema: orders
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS orders;

-- ── Cleaner Profiles ──────────────────────────────────────────
-- user_id references IAC iac_db.users(id) — no FK (cross-DB reference)
CREATE TABLE IF NOT EXISTS orders.cleaner_profiles (
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
-- client_id, property_id reference other service DBs — no FK (cross-DB)
CREATE TABLE IF NOT EXISTS orders.orders (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id          UUID          NOT NULL,   -- from IAC iac_db
    property_id        UUID          NOT NULL,   -- from property_db
    property_name      VARCHAR(100),             -- snapshot at order creation
    service_type_code  VARCHAR(50),              -- snapshot from pricing_db
    service_type_name  VARCHAR(100),             -- snapshot
    cleaner_id         UUID          REFERENCES orders.cleaner_profiles(id),
    frequency          VARCHAR(20)   NOT NULL DEFAULT 'ONE_TIME',
    scheduled_date     TIMESTAMP     NOT NULL,
    duration_hours     NUMERIC(5,2),
    base_price         NUMERIC(10,2) NOT NULL DEFAULT 0,
    discount_percent   NUMERIC(5,2)  NOT NULL DEFAULT 0,
    addons_price       NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_price        NUMERIC(10,2) NOT NULL DEFAULT 0,
    status             VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    cancellation_reason TEXT,
    client_notes       TEXT,
    addons_snapshot    TEXT,                     -- JSON snapshot of chosen addons
    created_at         TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Earnings ──────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders.earnings (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    cleaner_id  UUID          NOT NULL REFERENCES orders.cleaner_profiles(id),
    order_id    UUID          NOT NULL REFERENCES orders.orders(id),
    amount      NUMERIC(10,2) NOT NULL,
    paid_at     TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_orders_client      ON orders.orders(client_id);
CREATE INDEX IF NOT EXISTS idx_orders_cleaner     ON orders.orders(cleaner_id);
CREATE INDEX IF NOT EXISTS idx_orders_status      ON orders.orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_date        ON orders.orders(scheduled_date);
CREATE INDEX IF NOT EXISTS idx_earn_cleaner       ON orders.earnings(cleaner_id);
CREATE INDEX IF NOT EXISTS idx_cleaner_available  ON orders.cleaner_profiles(is_available, last_assigned_at);
