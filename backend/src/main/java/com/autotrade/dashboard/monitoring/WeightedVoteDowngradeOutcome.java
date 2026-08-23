package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.DirectionalOutcomeStats;

/**
 * The "downgraded-by-weighted" agreement bucket (E8-F5-S3): every entry actually recorded as BUY
 * or SELL that {@link com.autotrade.dashboard.signal.WeightedVoteRuleEngine} would have suppressed
 * to HOLD. Unlike {@link WeightedVoteBucketOutcome}, these entries are real recorded BUY/SELL
 * calls and so always carry their own persisted hold-term — scored at the entry's own MIN/MID/MAX
 * checkpoints (the same shape {@code monitoring.LiveSignalDriftService} already uses), pooled
 * across both directions into one {@link DirectionalOutcomeStats} rather than split by BUY/SELL,
 * matching this story's AC naming a single "downgraded-by-weighted" bucket, not two. {@code
 * WalkForwardScorer.score}'s {@code isBuy} flag already normalizes each entry's signed return to
 * "in the recorded call's own direction" before pooling, so a BUY win and a SELL win land on the
 * same signed scale — pooling is meaningful, not an apples-to-oranges average.
 *
 * <p>{@code count} is every entry sorted into this bucket; {@link DirectionalOutcomeStats
 * #totalCalls()} inside {@code scoring} can be smaller when an entry's own hold-term was somehow
 * missing (defensive-only — a real recorded BUY/SELL call always carries one) or its ticker's
 * forward market data couldn't be fetched.
 */
public record WeightedVoteDowngradeOutcome(int count, DirectionalOutcomeStats scoring) {
}
