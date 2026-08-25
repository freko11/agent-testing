-- System alerts (post-E8 follow-up) — an append-only ops-facing alert log, deliberately
-- separate from notifications (E5-F4-S1): that table DB-enforces every row is either an
-- order-outcome or a watchlist signal-change notification, both ticker-scoped. Neither a
-- kill-switch trip nor a live-signal-drift-decay event has a ticker, so this is a new
-- table rather than a relaxed constraint on notifications.
--
-- Two alert types, columns CHECK-enforced nullable per type (ck_system_alerts_fields):
-- KILL_SWITCH_ENGAGED references an existing, already-immutable kill_switch_events row;
-- SIGNAL_DRIFT_DECAY has no persisted source row (LiveSignalDriftService.computeDrift is
-- ephemeral/recomputed-per-call), so its rule-table-version/direction/checkpoint/drift-pct
-- are inlined snapshot values instead.

CREATE SEQUENCE system_alerts_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE system_alerts (
    id                    NUMBER(19)     NOT NULL,
    alert_type            VARCHAR2(30)   NOT NULL,
    message               VARCHAR2(500)  NOT NULL,
    kill_switch_event_id  NUMBER(19),
    rule_table_version    VARCHAR2(20),
    direction             VARCHAR2(4),
    checkpoint            VARCHAR2(3),
    drift_pct             NUMBER(10,4),
    created_at            TIMESTAMP(6)   DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_system_alerts PRIMARY KEY (id),
    CONSTRAINT fk_system_alerts_kill_switch_event FOREIGN KEY (kill_switch_event_id)
        REFERENCES kill_switch_events (id),
    CONSTRAINT ck_system_alerts_type CHECK (alert_type IN ('KILL_SWITCH_ENGAGED', 'SIGNAL_DRIFT_DECAY')),
    CONSTRAINT ck_system_alerts_direction CHECK (direction IS NULL OR direction IN ('BUY', 'SELL')),
    CONSTRAINT ck_system_alerts_checkpoint CHECK (checkpoint IS NULL OR checkpoint IN ('MIN', 'MID', 'MAX')),
    CONSTRAINT ck_system_alerts_fields CHECK (
        (alert_type = 'KILL_SWITCH_ENGAGED'
            AND kill_switch_event_id IS NOT NULL
            AND rule_table_version IS NULL AND direction IS NULL AND checkpoint IS NULL AND drift_pct IS NULL)
        OR
        (alert_type = 'SIGNAL_DRIFT_DECAY'
            AND kill_switch_event_id IS NULL
            AND rule_table_version IS NOT NULL AND direction IS NOT NULL
            AND checkpoint IS NOT NULL AND drift_pct IS NOT NULL)
    )
);

CREATE INDEX idx_system_alerts_created_at ON system_alerts (created_at);
CREATE INDEX idx_system_alerts_drift_dedupe
    ON system_alerts (alert_type, rule_table_version, direction, checkpoint, created_at);
