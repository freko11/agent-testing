package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.Checkpoint;

/**
 * The current {@code SignalRuleEngine.RULE_TABLE_VERSION} ("v2")'s already-computed BUY/SELL
 * {@code CheckpointStats.expectancyPctAfterCosts()} at MIN/MID/MAX — the reference point {@link
 * LiveSignalDriftService} compares live {@code OrderAuditEntry} performance against to surface
 * expectancy drift (E8-F5-S1's AC).
 *
 * <p><b>Where these numbers come from</b>: not stated cleanly as a single combined BUY/SELL
 * figure anywhere in docs/CHANGELOG.md's E8-F2-S1/E8-F2-S2 entries (those narrate the TP/SL
 * walk-forward scan and transaction-cost mechanics, with only a couple of per-rule spot figures
 * as illustration, not a full combined-across-fixtures BUY/SELL table) — so these were derived
 * directly, per this story's confirmed fallback: {@code BacktestHarness.run} against the two
 * checked-in BTCUSDT/DOGEUSDT fixtures (the same fixtures, same v2-thresholds {@code
 * RuleThresholds.DEFAULT}, every prior E8 calibration test uses), with {@code overallBuy()}/
 * {@code overallSell()} combined call-count-weighted across both fixtures — the exact same
 * combination formula (win/loss-count-weighted average of avg win/loss size) {@code
 * IndicatorExpectancyCalibrationTest} already established for {@code
 * WeightedVoteRuleEngine.IndicatorWeights.DEFAULT}, just applied to the combined-rule overall
 * BUY/SELL stats instead of a per-indicator one. {@link LiveDriftBaselineTest} re-derives and
 * pins these values down within a documented tolerance, the same "computed once, pinned as a
 * constant, guarded by a re-deriving test" pattern.
 *
 * <p>Scoped to only the CURRENT rule table version, per this story's confirmed scope — no
 * baseline exists (or is attempted) for v1 or any future version. If {@code RULE_TABLE_VERSION}
 * is ever bumped again, these constants become stale for newly-produced audit entries (an old
 * baseline compared against new-version live data) until a future story re-derives them; {@link
 * LiveSignalDriftService} does not attempt to detect or warn about that staleness itself.
 */
public final class LiveDriftBaseline {

    /** The rule table version these baseline figures were computed against — {@code
     * LiveSignalDriftService} only compares audit entries whose own {@code ruleTableVersion}
     * matches this, never a stale cross-version comparison. */
    public static final String RULE_TABLE_VERSION = "v2";

    public static final double BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS = -0.053166;
    public static final double BUY_MID_EXPECTANCY_PCT_AFTER_COSTS = 0.027064;
    public static final double BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS = 0.033141;

    public static final double SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS = -0.019962;
    public static final double SELL_MID_EXPECTANCY_PCT_AFTER_COSTS = 0.159881;
    public static final double SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS = 0.153708;

    private LiveDriftBaseline() {
    }

    public static double expectancyPctAfterCosts(boolean isBuy, Checkpoint checkpoint) {
        return switch (checkpoint) {
            case MIN -> isBuy ? BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS : SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS;
            case MID -> isBuy ? BUY_MID_EXPECTANCY_PCT_AFTER_COSTS : SELL_MID_EXPECTANCY_PCT_AFTER_COSTS;
            case MAX -> isBuy ? BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS : SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS;
        };
    }
}
