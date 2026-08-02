-- One-time risk-consent acknowledgment gating LIVE mode (E6-F1-S3), a second
-- independent gate alongside the paper-trade threshold (E6-F1-S2). Same
-- append-only "latest/only row = current state" audit pattern as
-- trading_mode_events. In practice this table holds at most one row —
-- giveRiskConsent() is idempotent — but stays append-only for consistency
-- with the rest of this codebase's audit-style tables (E6.3), not for any
-- planned re-consent/versioning feature.

CREATE SEQUENCE risk_consents_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE risk_consents (
    id             NUMBER(19)   NOT NULL,
    consented_at   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_risk_consents PRIMARY KEY (id)
);
