-- E4-F3-S2: BinanceFuturesTradingAdapter's leveraged bracket order can leave
-- an entry filled with a missing take-profit/stop-loss leg (rate limit,
-- rejection, timeout on the exit-leg call) -- a real open position with
-- incomplete protection, distinct from every existing status. Oracle
-- requires dropping and recreating a CHECK constraint to widen its value
-- list; no data migration needed since no row can carry the new value yet.
ALTER TABLE orders DROP CONSTRAINT ck_orders_status;

ALTER TABLE orders ADD CONSTRAINT ck_orders_status
    CHECK (status IN ('PENDING', 'SUBMITTED', 'FILLED', 'PARTIALLY_FILLED', 'REJECTED', 'CANCELLED', 'FAILED', 'PARTIALLY_PROTECTED'));
