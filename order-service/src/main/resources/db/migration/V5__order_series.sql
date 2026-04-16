-- V5: Add parent_order_id for recurring order series
ALTER TABLE orders.orders
    ADD COLUMN IF NOT EXISTS parent_order_id UUID REFERENCES orders.orders(id),
    ADD COLUMN IF NOT EXISTS series_index    INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS series_size     INT NOT NULL DEFAULT 1;

CREATE INDEX IF NOT EXISTS idx_orders_parent ON orders.orders(parent_order_id);
