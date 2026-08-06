package com.autotrade.dashboard.monitoring;

import java.util.List;

/**
 * Result of one {@link LiveSignalDriftService#computeDrift} call (E8-F5-S1) — ephemeral, never
 * persisted, recomputed fresh on every call per this story's confirmed scope.
 *
 * @param lookbackDays               the window this report replayed {@code OrderAuditEntry} rows
 *                                   over.
 * @param totalAuditEntriesConsidered every {@code FILLED}/{@code PARTIALLY_PROTECTED} audit entry
 *                                   found in the lookback window, before any per-ticker market-data
 *                                   failure is accounted for.
 * @param scoredAuditEntries         entries successfully fed into a {@code
 *                                   backtest.DirectionalAccumulator} — its own checkpoints may
 *                                   still be individually unscored if not enough forward time has
 *                                   passed yet, same as a {@code BacktestHarness} decision point
 *                                   too close to the end of its candle series.
 * @param skippedAuditEntries        entries skipped entirely (their ticker's market data couldn't
 *                                   be fetched, or their {@code SignalCallEntry} carries no
 *                                   hold-term) — logged, never silently dropped.
 * @param versions                   per-{@code rule_table_version} drift, one entry per version
 *                                   actually observed in the lookback window.
 */
public record SignalDriftReport(int lookbackDays, int totalAuditEntriesConsidered, int scoredAuditEntries,
                                 int skippedAuditEntries, List<RuleTableVersionDrift> versions) {
}
