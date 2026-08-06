package com.autotrade.dashboard.monitoring;

import java.util.List;

/**
 * One direction's (BUY or SELL) live drift, for one {@code rule_table_version} (E8-F5-S1).
 * {@code checkpoints} is empty whenever that rule table version doesn't match {@link
 * LiveDriftBaseline#RULE_TABLE_VERSION} — this story's confirmed scope covers only the current
 * version's baseline, so an older/newer version still surfaces its raw {@code totalCalls} (so a
 * near-empty audit log for that version reads as "no data yet", not "flat performance"), but
 * without a fabricated comparison against a baseline that was never computed for it.
 */
public record DirectionalDrift(int totalCalls, List<CheckpointDrift> checkpoints) {
}
