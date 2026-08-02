-- E6-F3-S2: denormalize the rule-table version onto order_audit_entries itself, right next to
-- the signal_call_id FK it's copied from, so a later SignalRuleEngine.RULE_TABLE_VERSION bump
-- never retroactively obscures which version produced a past order's signal. The table is
-- write-once (V13) and has no rows in any real environment yet, so a plain NOT NULL add is safe
-- with no default/backfill needed. Mirrors signal_calls.rule_table_version's own definition
-- (VARCHAR2(20) NOT NULL, no default, no CHECK -- free-form version string).

ALTER TABLE order_audit_entries
    ADD rule_table_version VARCHAR2(20) NOT NULL;
