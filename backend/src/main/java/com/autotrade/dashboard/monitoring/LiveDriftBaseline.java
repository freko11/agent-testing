package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.Checkpoint;

/**
 * The current {@code SignalRuleEngine.RULE_TABLE_VERSION} ("v4")'s already-computed BUY/SELL
 * {@code CheckpointStats.expectancyPctAfterCosts()} at MIN/MID/MAX — the reference point {@link
 * LiveSignalDriftService} compares live {@code OrderAuditEntry} performance against to surface
 * expectancy drift (E8-F5-S1's AC).
 *
 * <p><b>Where these numbers come from</b>: not stated cleanly as a single combined BUY/SELL
 * figure anywhere in docs/CHANGELOG.md's E8-F2-S1/E8-F2-S2 entries (those narrate the TP/SL
 * walk-forward scan and transaction-cost mechanics, with only a couple of per-rule spot figures
 * as illustration, not a full combined-across-fixtures BUY/SELL table) — so these were derived
 * directly, per this story's confirmed fallback: {@code BacktestHarness.run} against the two
 * checked-in BTCUSDT/DOGEUSDT fixtures (the same fixtures, same {@code RuleThresholds.DEFAULT}
 * thresholds, every prior E8 calibration test uses), with {@code overallBuy()}/
 * {@code overallSell()} combined call-count-weighted across both fixtures — the exact same
 * combination formula (win/loss-count-weighted average of avg win/loss size) {@code
 * IndicatorExpectancyCalibrationTest} already established for {@code
 * WeightedVoteRuleEngine.IndicatorWeights.DEFAULT}, just applied to the combined-rule overall
 * BUY/SELL stats instead of a per-indicator one. {@link LiveDriftBaselineTest} re-derives and
 * pins these values down within a documented tolerance, the same "computed once, pinned as a
 * constant, guarded by a re-deriving test" pattern.
 *
 * <p><b>E8-F1-S4's v2&rarr;v3 bump ({@code PerSymbolRuleThresholds}) required only a version-label
 * update here, not a re-derivation.</b> That story's only shipped override is SOLUSDT-specific;
 * BTCUSDT/DOGEUSDT — the only two fixtures this baseline is computed from — still resolve to
 * {@code RuleThresholds.DEFAULT} under v3 exactly as they did under v2, so {@code
 * BacktestHarness.run}'s output for them, and therefore every constant below, was byte-identical.
 * {@link LiveDriftBaselineTest} needed no changes and passed unmodified, confirming this.
 *
 * <p><b>E8-F3-S3's v3&rarr;v4 bump is different: the SELL constants were genuinely recomputed, not
 * just relabeled.</b> Wiring {@code RegimeGatedRuleEngine.applySellGate} into production means a
 * live v4 SELL audit entry can only ever be a trending-regime call (a ranging-regime SELL never
 * fires at all, for crypto tickers) — the pre-existing SELL constants below were derived from
 * BTCUSDT/DOGEUSDT's <em>ungated</em> {@code overallSell()}, which pools both regimes, so they no
 * longer describe what a v4 SELL call actually looks like. {@link LiveDriftBaselineTest} now calls
 * {@code BacktestHarness.run}'s gated overload ({@code applySellRegimeGate=true}) for the SELL
 * assertions specifically, and the SELL constants below were re-derived from that run's actual
 * output. BUY is unaffected by the gate (it never touches BUY calls) and the BUY constants below
 * are confirmed byte-identical to their v3 values by {@link LiveDriftBaselineTest}'s own unchanged
 * (ungated) BUY assertions.
 *
 * <p>Scoped to only the CURRENT rule table version, per this story's confirmed scope — no
 * baseline exists (or is attempted) for v1/v2/v3 or any future version. If {@code
 * RULE_TABLE_VERSION} is ever bumped again by a change that alters BTCUSDT/DOGEUSDT's own
 * resolved behavior, these constants become stale for newly-produced audit entries until a future
 * story re-derives them; {@link LiveSignalDriftService} does not attempt to detect or warn about
 * that staleness itself.
 */
public final class LiveDriftBaseline {

    /** The rule table version these baseline figures were computed against — {@code
     * LiveSignalDriftService} only compares audit entries whose own {@code ruleTableVersion}
     * matches this, never a stale cross-version comparison. */
    public static final String RULE_TABLE_VERSION = "v4";

    public static final double BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS = -0.053166;
    public static final double BUY_MID_EXPECTANCY_PCT_AFTER_COSTS = 0.027064;
    public static final double BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS = 0.033141;

    /** E8-F3-S3: recomputed against the gated ({@code applySellRegimeGate=true}) SELL behavior —
     * see class Javadoc. Every checkpoint moved up from its v3 (ungated) value, since dropping the
     * ranging-regime SELL calls (E8-F4-S2 found ranging SELL expectancy consistently worse than
     * trending on both fixtures) removes exactly the weaker-performing calls from the pool. */
    public static final double SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS = 0.033652;
    public static final double SELL_MID_EXPECTANCY_PCT_AFTER_COSTS = 0.180769;
    public static final double SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS = 0.222951;

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
