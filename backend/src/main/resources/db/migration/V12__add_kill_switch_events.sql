-- Kill switch (E6-F2-S2) — a single global control that blocks new order submissions
-- and best-effort cancels every open order on both adapters. Append-only history, same
-- "latest row = current state" pattern as trading_mode_events (V10) and risk_consents
-- (V11). An empty table means "never engaged" — KillSwitchService defaults that case to
-- CLEARED in application code, not seeded here, same as V10's convention.

CREATE SEQUENCE kill_switch_events_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Column named kill_switch_state, not the bare "state" — not asserting STATE is an
-- Oracle reserved word, just avoiding the risk given the documented MODE gotcha (V10).
CREATE TABLE kill_switch_events (
    id                 NUMBER(19)   NOT NULL,
    kill_switch_state  VARCHAR2(10) NOT NULL,
    changed_at         TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    changed_by         VARCHAR2(100),
    CONSTRAINT pk_kill_switch_events PRIMARY KEY (id),
    CONSTRAINT ck_kill_switch_events_state CHECK (kill_switch_state IN ('ENGAGED', 'CLEARED'))
);

CREATE INDEX idx_kill_switch_events_changed_at ON kill_switch_events (changed_at);
