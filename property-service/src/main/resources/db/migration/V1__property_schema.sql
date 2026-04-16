-- ============================================================
-- Property Service — Schema Migration (V1)
-- Database: property_db
-- Schema: property
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE SCHEMA IF NOT EXISTS property;

CREATE TABLE IF NOT EXISTS property.properties (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id       UUID        NOT NULL,       -- user_id from IAC (no FK — cross-DB)
    name            VARCHAR(100),
    type            VARCHAR(30) NOT NULL,        -- APARTMENT, HOUSE, OFFICE
    area_sqm        NUMERIC(8,2) NOT NULL,
    bathrooms_count INT         NOT NULL DEFAULT 1,
    address         TEXT,
    notes           TEXT,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prop_client ON property.properties(client_id);
CREATE INDEX IF NOT EXISTS idx_prop_active ON property.properties(client_id, active);
