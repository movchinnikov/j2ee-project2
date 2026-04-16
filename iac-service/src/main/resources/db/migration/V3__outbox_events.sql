-- ============================================================
-- V3__outbox_events.sql
-- Transactional Outbox pattern table for IAC Service.
-- Events are written here atomically with business transactions,
-- then relayed to Kafka by OutboxRelayScheduler.
-- ============================================================

CREATE TABLE IF NOT EXISTS outbox_events (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,   -- e.g. 'User'
    aggregate_id   VARCHAR(255) NOT NULL,   -- UUID of the entity
    event_type     VARCHAR(100) NOT NULL,   -- e.g. 'user.registered'
    payload        TEXT         NOT NULL,   -- JSON-serialised event DTO
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | PROCESSED | FAILED
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP    NULL,
    error_message  TEXT         NULL        -- populated on FAILED
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_created
    ON outbox_events (status, created_at);
