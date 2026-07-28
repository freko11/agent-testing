-- Suggested hold-term (E2-F3-S2) alongside each signal_calls row. Nullable: a HOLD call
-- has no hold-term (no entry to size a horizon for). Stored, not recomputed at read time,
-- because it's versioned exactly like rule_table_version — a later revision of the
-- hold-term day-range table must not silently reinterpret a past audit row.

ALTER TABLE signal_calls ADD (
    hold_term_min_days       NUMBER(4),
    hold_term_max_days       NUMBER(4),
    hold_term_table_version  VARCHAR2(20)
);

ALTER TABLE signal_calls ADD CONSTRAINT ck_signal_calls_hold_term CHECK (
    (hold_term_min_days IS NULL AND hold_term_max_days IS NULL AND hold_term_table_version IS NULL)
    OR (hold_term_min_days IS NOT NULL AND hold_term_max_days IS NOT NULL AND hold_term_table_version IS NOT NULL
        AND hold_term_min_days > 0 AND hold_term_min_days <= hold_term_max_days)
);
