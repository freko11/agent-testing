-- Buy/Sell/Hold rule-engine output (E2-F3-S1) — the signal_calls table
-- indicator_snapshots' own comment anticipated. Append-only, no unique
-- constraint on ticker+day, same audit-log-style pattern as
-- indicator_snapshots. ticker_id is denormalized alongside
-- indicator_snapshot_id (reachable via that FK) matching orders' own
-- existing precedent of carrying both.

CREATE SEQUENCE signal_calls_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE signal_calls (
    id                      NUMBER(19)      NOT NULL,
    ticker_id               NUMBER(19)      NOT NULL,
    indicator_snapshot_id   NUMBER(19)      NOT NULL,
    call                    VARCHAR2(10)    NOT NULL,
    rule_table_version      VARCHAR2(20)    NOT NULL,
    matched_rule            VARCHAR2(30)    NOT NULL,
    created_at              TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_signal_calls PRIMARY KEY (id),
    CONSTRAINT fk_signal_calls_ticker FOREIGN KEY (ticker_id) REFERENCES tickers (id),
    CONSTRAINT fk_signal_calls_snapshot FOREIGN KEY (indicator_snapshot_id) REFERENCES indicator_snapshots (id),
    CONSTRAINT ck_signal_calls_call CHECK (call IN ('BUY', 'SELL', 'HOLD'))
);

CREATE INDEX idx_signal_calls_ticker_id ON signal_calls (ticker_id);
CREATE INDEX idx_signal_calls_created_at ON signal_calls (created_at);
