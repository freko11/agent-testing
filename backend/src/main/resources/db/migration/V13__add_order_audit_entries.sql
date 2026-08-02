-- E6-F3-S1: immutable audit trail of every order decision and the signal that triggered it.
-- Unlike orders (mutated in place by applyOutcome/refreshOrder as status resolves further),
-- this table is write-once per order -- OrderService.submitOrder inserts exactly one row, at
-- the order's first resolved outcome, and nothing ever updates it afterward.
-- result_status's allowed values must stay in sync with orders.ck_orders_status (V8) --
-- widening one without the other is a bug.

CREATE SEQUENCE order_audit_entries_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE order_audit_entries (
    id                 NUMBER(19)    NOT NULL,
    order_id           NUMBER(19)    NOT NULL,
    ticker_id          NUMBER(19)    NOT NULL,
    signal_call_id     NUMBER(19)    NOT NULL,
    result_status      VARCHAR2(20)  NOT NULL,
    rejection_reason   VARCHAR2(500),
    broker_order_id    VARCHAR2(64),
    entry_price        NUMBER(20,8),
    logged_at          TIMESTAMP(6)  DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_order_audit_entries PRIMARY KEY (id),
    CONSTRAINT uk_order_audit_entries_order UNIQUE (order_id),
    CONSTRAINT fk_order_audit_entries_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_audit_entries_ticker FOREIGN KEY (ticker_id) REFERENCES tickers (id),
    CONSTRAINT fk_order_audit_entries_signal_call FOREIGN KEY (signal_call_id) REFERENCES signal_calls (id),
    CONSTRAINT ck_order_audit_entries_result CHECK (result_status IN
        ('PENDING', 'SUBMITTED', 'FILLED', 'PARTIALLY_FILLED', 'REJECTED', 'CANCELLED', 'FAILED',
         'PARTIALLY_PROTECTED', 'SUBMISSION_UNKNOWN'))
);

CREATE INDEX idx_order_audit_entries_ticker_id ON order_audit_entries (ticker_id);
CREATE INDEX idx_order_audit_entries_logged_at ON order_audit_entries (logged_at);
