package com.autotrade.dashboard.monitoring;

/**
 * One {@code rule_table_version}'s live drift, both directions (E8-F5-S1). {@code hasBaseline}
 * is {@code true} only for {@link LiveDriftBaseline#RULE_TABLE_VERSION} — the only version this
 * story computed a baseline for.
 */
public record RuleTableVersionDrift(String ruleTableVersion, boolean hasBaseline, DirectionalDrift buy,
                                     DirectionalDrift sell) {
}
