package com.autotrade.dashboard.monitoring;

import java.util.List;

/**
 * Result of one {@link WeightedVoteShadowScoringService#computeShadowReport} call (E8-F5-S3) —
 * ephemeral, never persisted, recomputed fresh on every call, same treatment as {@code
 * monitoring.SignalDriftReport}.
 *
 * @param lookbackDays          the window this report replayed {@code SignalCallEntry} rows over.
 * @param totalEntriesConsidered every {@code SignalCallEntry} found in the lookback window, before
 *                              any per-entry reconstruction failure is accounted for.
 * @param skippedEntries        entries this report couldn't even classify into a bucket (its
 *                              stored indicator values couldn't be turned back into {@code
 *                              WeightedVoteRuleEngine.evaluate}'s inputs) — logged, never silently
 *                              dropped. Always equals {@code totalEntriesConsidered - (agreeCount +
 *                              weightedOnlyBuy.count() + weightedOnlySell.count() +
 *                              downgradedByWeighted.count())}.
 * @param agreeCount            entries where the weighted engine resolves the exact same {@code
 *                              SignalCall} that was actually recorded (including HOLD == HOLD) —
 *                              not walk-forward scored, per this story's AC (only disagreements are).
 * @param weightedOnlyBuy       entries the weighted engine would call BUY that weren't recorded as BUY.
 * @param weightedOnlySell      entries the weighted engine would call SELL that weren't recorded as SELL.
 * @param downgradedByWeighted  entries recorded as BUY or SELL that the weighted engine would have
 *                              suppressed to HOLD.
 * @param knownLimitations      documented, not silently omitted, gaps in what this replay can
 *                              measure — see {@link WeightedVoteShadowScoringService}'s class
 *                              Javadoc for the full explanation of each one.
 */
public record WeightedVoteShadowReport(int lookbackDays, int totalEntriesConsidered, int skippedEntries,
                                        int agreeCount, WeightedVoteBucketOutcome weightedOnlyBuy,
                                        WeightedVoteBucketOutcome weightedOnlySell,
                                        WeightedVoteDowngradeOutcome downgradedByWeighted,
                                        List<String> knownLimitations) {
}
