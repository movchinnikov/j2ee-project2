-- ============================================================
-- V3__outbox_events.sql
-- Transactional Outbox pattern table for Order Service.
-- Events are written atomically with business transactions,
-- then relayed to Kafka by OutboxRelayScheduler.
-- ============================================================

CREATE TABLE IF NOT EXISTS orders.outbox_events (
    id             UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    processed_at   TIMESTAMP    NULL,
    error_message  TEXT         NULL
);

CREATE INDEX IF NOT EXISTS idx_order_outbox_status_created
    ON orders.outbox_events (status, created_at);
