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
 *
 * <p><b>E8-F1-S8's v4&rarr;v5 bump required only a version-label update here, not a
 * re-derivation</b> — the same treatment E8-F1-S4's own v2&rarr;v3 bump got. That story's only
 * shipped override ({@code macdMinHistogramMagnitudePct = 0.10}, composed alongside the existing
 * {@code rsiOverbought = 70}) is SOLUSDT-specific; BTCUSDT/DOGEUSDT — the only two fixtures this
 * baseline is computed from — still resolve to {@code RuleThresholds.DEFAULT} under v5 exactly as
 * they did under v4, so {@code BacktestHarness.run}'s output for them, and therefore every
 * constant below, is byte-identical. {@link LiveDriftBaselineTest} needed no changes and passed
 * unmodified, confirming this.
 *
 * <p><b>E8-F1-S11's v5&rarr;v6 bump is like E8-F3-S3's, not E8-F1-S8's: the SELL constants were
 * genuinely recomputed again, not just relabeled.</b> Wiring {@code MaCrossoverSellGate#applySellGate}
 * into production alongside the already-wired regime gate means a live v6 SELL audit entry can
 * only ever be a trending-regime call whose MA-crossover separation also clears 2.00% — the v5
 * SELL constants were derived from the regime-gated-only {@code overallSell()}, which no longer
 * describes what a v6 SELL call looks like. {@link LiveDriftBaselineTest} now calls {@code
 * BacktestHarness.run}'s 7-arg overload with both {@code applySellRegimeGate=true} and {@code
 * applyMaCrossoverSellGate=true} for the SELL assertions, and the SELL constants below were
 * re-derived from that run's actual output — every checkpoint moved up again from its v5 value
 * (MIN 0.033652&rarr;0.067244, MID 0.180769&rarr;0.165253, MAX 0.222951&rarr;0.241016), consistent
 * with dropping more of the weaker-performing SELL calls from the pool, though MID moved down
 * slightly rather than up (the two gates don't remove a strictly nested set of calls — layering a
 * second, independent filter on top of the first can shift a weighted average either direction at
 * a given checkpoint even while the other two move up). BUY is unaffected by both gates (neither
 * ever touches BUY calls) and the BUY constants below are confirmed byte-identical to their v5
 * values by {@link LiveDriftBaselineTest}'s own unchanged (ungated) BUY assertions.
 *
 * <p><b>E8-F5-S2 added funding-adjusted counterparts</b> ({@code
 * *_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING}) to every constant above, computed the exact same way
 * (same fixtures, same gates, same call-count-weighted combine) but reading {@code
 * CheckpointStats.expectancyPctAfterCostsAndFunding()} instead of {@code
 * expectancyPctAfterCosts()} — the follow-up E8-F2-S3 itself named ("wiring the funding-adjusted
 * figure into live drift monitoring is a real, separate future story, not folded in here"). Every
 * funding-adjusted BUY constant lands well below its cost-only sibling and turns net negative at
 * every checkpoint (funding cost scales with {@code avgHoldingDays}, and BUY calls in these
 * fixtures hold on average 1.5-2.9 days across MIN/MID/MAX); SELL's funding-adjusted MIN/MID
 * constants are negative even though their cost-only siblings are positive (SELL calls hold
 * slightly less long on average, 1.3-2.3 days, but not enough to avoid funding erasing the
 * cost-only edge at the shorter checkpoints — only MAX stays positive after funding). {@link
 * LiveDriftBaselineTest} re-derives and pins these down the same way as the cost-only constants.
 */
public final class LiveDriftBaseline {

    /** The rule table version these baseline figures were computed against — {@code
     * LiveSignalDriftService} only compares audit entries whose own {@code ruleTableVersion}
     * matches this, never a stale cross-version comparison. */
    public static final String RULE_TABLE_VERSION = "v6";

    public static final double BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS = -0.053166;
    public static final double BUY_MID_EXPECTANCY_PCT_AFTER_COSTS = 0.027064;
    public static final double BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS = 0.033141;

    /** E8-F5-S2: funding-adjusted counterparts to the three constants above — see class Javadoc. */
    public static final double BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING = -0.192350;
    public static final double BUY_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING = -0.203779;
    public static final double BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING = -0.226813;

    /** E8-F1-S11: recomputed against both gates' combined behavior ({@code
     * applySellRegimeGate=true}, {@code applyMaCrossoverSellGate=true}) — see class Javadoc. Every
     * checkpoint moved up again from its v4/v5 (regime-gated-only) value, since layering the
     * MA-crossover separation gate on top removes more of the weaker-performing SELL calls from the
     * pool, the same direction E8-F3-S3's own v3&rarr;v4 recomputation moved. */
    public static final double SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS = 0.067244;
    public static final double SELL_MID_EXPECTANCY_PCT_AFTER_COSTS = 0.165253;
    public static final double SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS = 0.241016;

    /** E8-F5-S2: funding-adjusted counterparts to the three constants above — see class Javadoc.
     * Recomputed against the same both-gates-applied run as the cost-only SELL constants above. */
    public static final double SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING = -0.052442;
    public static final double SELL_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING = -0.029354;
    public static final double SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING = 0.031330;

    private LiveDriftBaseline() {
    }

    public static double expectancyPctAfterCosts(boolean isBuy, Checkpoint checkpoint) {
        return switch (checkpoint) {
            case MIN -> isBuy ? BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS : SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS;
            case MID -> isBuy ? BUY_MID_EXPECTANCY_PCT_AFTER_COSTS : SELL_MID_EXPECTANCY_PCT_AFTER_COSTS;
            case MAX -> isBuy ? BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS : SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS;
        };
    }

    /** E8-F5-S2: funding-adjusted counterpart to {@link #expectancyPctAfterCosts(boolean,
     * Checkpoint)}. */
    public static double expectancyPctAfterCostsAndFunding(boolean isBuy, Checkpoint checkpoint) {
        return switch (checkpoint) {
            case MIN -> isBuy ? BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING : SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING;
            case MID -> isBuy ? BUY_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING : SELL_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING;
            case MAX -> isBuy ? BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING : SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING;
        };
    }
}
