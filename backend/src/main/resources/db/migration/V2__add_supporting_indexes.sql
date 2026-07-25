-- Supporting indexes for the orders table (E1-F2-S3): proves incremental
-- Flyway migrations work on top of V1, and covers the access patterns we
-- already know we need — status polling/history views, per-ticker order
-- lookups, and paper/live history queries scoped by creation time.

CREATE INDEX idx_orders_ticker_id ON orders (ticker_id);
CREATE INDEX idx_orders_status ON orders (status);
CREATE INDEX idx_orders_mode_created_at ON orders (order_mode, created_at);
