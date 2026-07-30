-- Global paper/live trading-mode switch (E6-F1-S1) — append-only history of
-- mode changes; "current" mode is always the latest row by id, the same
-- "latest row = current state" pattern signal_calls already established.
-- An empty table means "never explicitly switched" — TradingModeService
-- defaults that case to PAPER in application code, not seeded here, so a
-- fresh install's lack of history stays honestly inspectable.

CREATE SEQUENCE trading_mode_events_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Column is named trading_mode, not the bare "mode" — MODE is an Oracle SQL
-- reserved keyword (used in LOCK TABLE ... MODE / ALTER SESSION ... MODE
-- syntax); a column literally named mode fails real Oracle with ORA-00904
-- ("invalid identifier"), a real-Oracle-only bug H2's Oracle-compatibility
-- mode does not catch since H2 doesn't reserve that word.
CREATE TABLE trading_mode_events (
    id             NUMBER(19)   NOT NULL,
    trading_mode   VARCHAR2(10) NOT NULL,
    changed_at     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_trading_mode_events PRIMARY KEY (id),
    CONSTRAINT ck_trading_mode_events_mode CHECK (trading_mode IN ('PAPER', 'LIVE'))
);

CREATE INDEX idx_trading_mode_events_changed_at ON trading_mode_events (changed_at);
