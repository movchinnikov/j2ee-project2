-- ============================================================
-- PostgreSQL initialization script
-- Creates one database per service, each with a dedicated user.
-- All databases live on the shared PostgreSQL instance.
-- Runs automatically when the postgres container is first started.
-- ============================================================

-- ── IAC Service ───────────────────────────────────────────────
-- iac_db already exists (POSTGRES_DB=iac_db), user already exists
-- Just ensure the setup is correct
ALTER USER iac_user WITH PASSWORD 'iac_secret_pass';
GRANT ALL PRIVILEGES ON DATABASE iac_db TO iac_user;

-- ── Property Service ──────────────────────────────────────────
SELECT 'CREATE DATABASE property_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'property_db')\gexec

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'property_user') THEN
    CREATE USER property_user WITH PASSWORD 'property_secret_pass';
  END IF;
END$$;

GRANT ALL PRIVILEGES ON DATABASE property_db TO property_user;

-- ── Pricing Service ───────────────────────────────────────────
SELECT 'CREATE DATABASE pricing_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'pricing_db')\gexec

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'pricing_user') THEN
    CREATE USER pricing_user WITH PASSWORD 'pricing_secret_pass';
  END IF;
END$$;

GRANT ALL PRIVILEGES ON DATABASE pricing_db TO pricing_user;

-- ── Order Service ─────────────────────────────────────────────
SELECT 'CREATE DATABASE order_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'order_db')\gexec

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'order_user') THEN
    CREATE USER order_user WITH PASSWORD 'order_secret_pass';
  END IF;
END$$;

GRANT ALL PRIVILEGES ON DATABASE order_db TO order_user;

-- ── Booking Service ───────────────────────────────────────────
SELECT 'CREATE DATABASE booking_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'booking_db')\gexec

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'booking_user') THEN
    CREATE USER booking_user WITH PASSWORD 'booking_secret_pass';
  END IF;
END$$;

GRANT ALL PRIVILEGES ON DATABASE booking_db TO booking_user;
