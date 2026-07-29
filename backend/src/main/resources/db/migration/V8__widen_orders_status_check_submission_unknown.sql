-- E5-F2-S1: an order-submission call can end in genuine ambiguity (RetryingBrokerAdapter's
-- placeOrder reconciliation itself fails, so it's unknown whether the broker received the
-- order) -- distinct from FAILED (broker confirmed nothing was submitted, safe to retry).
-- Oracle requires dropping and recreating a CHECK constraint to widen its value list; no
-- data migration needed since no row can carry the new value yet.
ALTER TABLE orders DROP CONSTRAINT ck_orders_status;

ALTER TABLE orders ADD CONSTRAINT ck_orders_status
    CHECK (status IN ('PENDING', 'SUBMITTED', 'FILLED', 'PARTIALLY_FILLED', 'REJECTED', 'CANCELLED', 'FAILED', 'PARTIALLY_PROTECTED', 'SUBMISSION_UNKNOWN'));
