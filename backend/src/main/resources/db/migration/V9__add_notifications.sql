-- Notifications (E5-F4-S1) — an append-only in-app notification list, same
-- audit-log-style pattern as indicator_snapshots/signal_calls: no unique
-- constraint, immutable except the one intentionally-mutable read_at column.
-- Each row is either an order-outcome notification (order_id set) or a
-- watchlist signal-change notification (signal_call_id set) — never both,
-- enforced by ck_notifications_association, the same defense-in-depth style
-- as V1's stock/leverage check and V5's hold-term all-or-nothing check.

CREATE SEQUENCE notifications_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE notifications (
    id               NUMBER(19)      NOT NULL,
    ticker_id        NUMBER(19)      NOT NULL,
    order_id         NUMBER(19),
    signal_call_id   NUMBER(19),
    event_type       VARCHAR2(30)    NOT NULL,
    message          VARCHAR2(500)   NOT NULL,
    read_at          TIMESTAMP(6),
    created_at       TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id),
    CONSTRAINT fk_notifications_ticker FOREIGN KEY (ticker_id) REFERENCES tickers (id),
    CONSTRAINT fk_notifications_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_notifications_signal_call FOREIGN KEY (signal_call_id) REFERENCES signal_calls (id),
    CONSTRAINT ck_notifications_association CHECK (
        (order_id IS NOT NULL AND signal_call_id IS NULL) OR
        (order_id IS NULL AND signal_call_id IS NOT NULL)
    )
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);
