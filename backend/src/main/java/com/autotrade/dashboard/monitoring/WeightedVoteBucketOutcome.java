package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.CheckpointStats;

/**
 * One direction's "weighted-only" agreement bucket (E8-F5-S3): every entry the recorded call
 * missed but {@link com.autotrade.dashboard.signal.WeightedVoteRuleEngine} would have called BUY
 * or SELL. {@code count} is every entry sorted into this bucket, regardless of whether market
 * data was available to score it; {@code scoring} is the walk-forward outcome over however many
 * of those entries actually could be scored (its own {@code scored()}/{@code notScored()} can be
 * smaller than {@code count} when a ticker's forward market data couldn't be fetched — see
 * {@link WeightedVoteShadowScoringService}'s class Javadoc).
 *
 * <p>Scored at a single fixed horizon ({@code backtest.BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}),
 * not a MIN/MID/MAX hold-term range, because these entries were recorded as HOLD and carry no
 * persisted hold-term to derive a range from — the same "no hold-term of its own" situation {@code
 * BacktestHarness} already handles for its own per-indicator/hold-gate scoring, at the same
 * reference horizon.
 */
public record WeightedVoteBucketOutcome(int count, CheckpointStats scoring) {
}
