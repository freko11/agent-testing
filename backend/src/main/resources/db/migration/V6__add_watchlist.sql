-- Watchlist (E3-F3-S1) — saved tickers a user wants to revisit without
-- retyping. Single-user app (no user/account table, per E1-F3-S2's design),
-- so this is a flat list, not scoped to an account. One row per ticker: a
-- ticker can only appear on the watchlist once (uq_watchlist_entries_ticker),
-- matching TickerService.resolveOrRegister's own idempotent-add precedent.

CREATE SEQUENCE watchlist_entries_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE watchlist_entries (
    id          NUMBER(19)      NOT NULL,
    ticker_id   NUMBER(19)      NOT NULL,
    created_at  TIMESTAMP(6)    DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_watchlist_entries PRIMARY KEY (id),
    CONSTRAINT uq_watchlist_entries_ticker UNIQUE (ticker_id),
    CONSTRAINT fk_watchlist_entries_ticker FOREIGN KEY (ticker_id) REFERENCES tickers (id)
);
