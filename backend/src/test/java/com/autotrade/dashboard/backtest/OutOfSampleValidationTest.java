package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.IndicatorId;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine.IndicatorWeights;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F4-S1: out-of-sample validation of E8-F1-S1's RSI 25/75 threshold shift and E8-F3-S1's
 * {@link IndicatorWeights#DEFAULT} — both were tuned <i>and</i> evaluated on the same two
 * checked-in BTCUSDT/DOGEUSDT fixtures {@link ThresholdCalibrationTest} and {@code
 * IndicatorExpectancyCalibrationTest} still use, which is exactly the "tuned and tested on the
 * same fixture" gap this story exists to close.
 *
 * <p>Two independent out-of-sample checks, per the AC's "a held-out split <i>or</i> additional
 * untouched fixture symbol/period" (both are used here, not just one):
 * <ol>
 *   <li><b>Chronological split</b> within BTCUSDT/DOGEUSDT: the first {@link #SPLIT_INDEX}
 *   candles (~70%, Nov 2023 onward) are the original tuning window every prior calibration used;
 *   the remaining ~30% (the newest ~260-290 candles) is held out and untouched by {@code
 *   ThresholdCalibrationTest}/{@code IndicatorExpectancyCalibrationTest}. Tune-on-earlier /
 *   hold-out-on-later, never reversed — a real deployment only ever sees data after whatever
 *   period a rule table was tuned on.
 *   <p><b>Anchor-discontinuity caveat:</b> {@link BacktestHarness} computes every indicator over
 *   a growing window anchored at index 0 of whatever list it's given (see its own class Javadoc).
 *   RSI's Wilder average and MACD's EMA therefore re-seed at the held-out slice's start instead of
 *   carrying forward continuous history, so the first ~30-50 candles of the held-out slice compute
 *   slightly different indicator values than production would have computed on those same
 *   calendar days. This decays exponentially and is negligible over the ~260-290-candle held-out
 *   window used here, but is a real methodological caveat, not swept under the rug.</li>
 *   <li><b>A genuine third fixture</b>, SOLUSDT — a symbol neither calibration has ever seen, same
 *   Nov 2023 - Jul 2026 daily period as BTCUSDT/DOGEUSDT (isolates "does this generalize to
 *   another asset" as the one new variable, rather than also varying the time period), fetched
 *   the same way the original two fixtures were built (Binance's public klines endpoint, no
 *   auth).</li>
 * </ol>
 *
 * <p><b>Bar for "holds out of sample"</b> — the same bar {@code ThresholdCalibrationTest}'s own
 * E8-F1-S1 finding already applied ("a larger scored sample at each step... not an overfit to a
 * handful of points"): direction-of-effect plus adequate {@code n}, not exact-magnitude matching.
 * RSI 25/75 should show equal-or-better win rate/expectancy than the pre-tuning 30/70 baseline on
 * held-out data, with a comparably large scored {@code n}; the three indicators' held-out combined
 * after-cost expectancy should still be consistent with {@link IndicatorWeights#DEFAULT}'s current
 * all-zero calibration (i.e. still {@code <= 0}) — or, if not, that mismatch is reported as a
 * finding, not silently ignored.
 *
 * <p><b>Scope boundary (confirmed with the user before implementation):</b> this story validates
 * existing shipped/computed values — it does not itself change {@code RULE_TABLE_VERSION},
 * {@code SignalRuleEngine}'s production RSI thresholds, or {@link IndicatorWeights#DEFAULT}. If
 * the held-out evidence does not confirm a prior finding, that is reported here (see
 * docs/CHANGELOG.md's E8-F4-S1 entry for the actual result) and left as a flagged finding for a
 * deliberate, separately-versioned follow-up story — the same treatment E8-F3-S2 gave its own
 * mixed regime evidence, rather than silently reverting a shipped value inside a story whose AC is
 * "validate", not "re-tune". E8-F3-S2's own regime-filter threshold is out of scope here (not
 * named in this story's AC, and its calibration was already fixture-mixed rather than a clean
 * value to validate).
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review, not a regression target. Read the printed output (rerun
 * via {@code ./mvnw test -Dtest=OutOfSampleValidationTest}) for the actual figures.
 */
class OutOfSampleValidationTest {

    private static final List<Candle> SOLUSDT = FixtureSplits.SOLUSDT;

    /** ~70% of each 1000-candle fixture (Nov 2023 - Jul 2026) is the original tuning window; the
     * remaining ~30% is held out here for the first time by any E8 calibration. Now shared via
     * {@link FixtureSplits}, see that class's Javadoc. */
    private static final int SPLIT_INDEX = FixtureSplits.SPLIT_INDEX;

    private static final List<Candle> BTCUSDT_HELD_OUT = FixtureSplits.BTCUSDT_HELD_OUT;
    private static final List<Candle> DOGEUSDT_HELD_OUT = FixtureSplits.DOGEUSDT_HELD_OUT;

    private static final RuleThresholds CURRENT_V2 = RuleThresholds.DEFAULT; // 25/75, shipped by E8-F1-S1
    private static final RuleThresholds PRE_TUNING_V1 = new RuleThresholds(
            new BigDecimal("30"), new BigDecimal("70"), CURRENT_V2.volatilityExtreme(), CURRENT_V2.volumeDriedUp(),
            CURRENT_V2.macdMinHistogramMagnitudePct(), CURRENT_V2.maMinSeparationPctOfPrice());

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void rsiThresholdHoldsOutOfSample() {
        System.out.println();
        System.out.println("########## E8-F4-S1: RSI 25/75 (v2) vs. 30/70 (pre-tuning) on held-out data ##########");

        runAndPrint("BTCUSDT [held-out tail]", BTCUSDT_HELD_OUT, "25/75 (v2, current)", CURRENT_V2);
        runAndPrint("BTCUSDT [held-out tail]", BTCUSDT_HELD_OUT, "30/70 (pre-tuning)", PRE_TUNING_V1);
        runAndPrint("DOGEUSDT [held-out tail]", DOGEUSDT_HELD_OUT, "25/75 (v2, current)", CURRENT_V2);
        runAndPrint("DOGEUSDT [held-out tail]", DOGEUSDT_HELD_OUT, "30/70 (pre-tuning)", PRE_TUNING_V1);
        runAndPrint("SOLUSDT [untouched fixture]", SOLUSDT, "25/75 (v2, current)", CURRENT_V2);
        runAndPrint("SOLUSDT [untouched fixture]", SOLUSDT, "30/70 (pre-tuning)", PRE_TUNING_V1);
    }

    @Test
    void indicatorWeightsHoldOutOfSample() {
        System.out.println();
        System.out.println("########## E8-F4-S1: per-indicator expectancy on held-out data (vs. IndicatorWeights.DEFAULT) ##########");

        BacktestReport btcHeldOut = BacktestHarness.run("BTCUSDT [held-out tail]", BTCUSDT_HELD_OUT);
        BacktestReport dogeHeldOut = BacktestHarness.run("DOGEUSDT [held-out tail]", DOGEUSDT_HELD_OUT);
        BacktestReport sol = BacktestHarness.run("SOLUSDT [untouched fixture]", SOLUSDT);

        assertStructurallySane(btcHeldOut);
        assertStructurallySane(dogeHeldOut);
        assertStructurallySane(sol);

        for (IndicatorId indicatorId : IndicatorId.values()) {
            CheckpointStats btcStats = btcHeldOut.indicatorStats().get(indicatorId);
            CheckpointStats dogeStats = dogeHeldOut.indicatorStats().get(indicatorId);
            CheckpointStats solStats = sol.indicatorStats().get(indicatorId);
            CheckpointStats combined = BacktestHarness.combineCheckpoint(
                    BacktestHarness.combineCheckpoint(btcStats, dogeStats), solStats);

            System.out.printf("%n%s:%n", indicatorId);
            printIndicatorLine("  BTCUSDT [held-out] ", btcStats);
            printIndicatorLine("  DOGEUSDT [held-out]", dogeStats);
            printIndicatorLine("  SOLUSDT [untouched]", solStats);
            printIndicatorLine("  COMBINED            ", combined);
            System.out.printf("  -> out-of-sample weight = max(0, %.4f) = %.4f (tuning-set IndicatorWeights.DEFAULT was %s)%n",
                    combined.expectancyPctAfterCosts(), Math.max(0.0, combined.expectancyPctAfterCosts()),
                    tuningSetWeightFor(indicatorId));

            assertIndicatorStatsSane(indicatorId.name() + " BTCUSDT", btcStats);
            assertIndicatorStatsSane(indicatorId.name() + " DOGEUSDT", dogeStats);
            assertIndicatorStatsSane(indicatorId.name() + " SOLUSDT", solStats);
        }
    }

    private BigDecimal tuningSetWeightFor(IndicatorId indicatorId) {
        return switch (indicatorId) {
            case RSI -> IndicatorWeights.DEFAULT.rsiWeight();
            case MACD -> IndicatorWeights.DEFAULT.macdWeight();
            case MA_CROSSOVER -> IndicatorWeights.DEFAULT.maCrossoverWeight();
        };
    }

    private void runAndPrint(String symbolLabel, List<Candle> candles, String thresholdLabel, RuleThresholds thresholds) {
        BacktestReport report = BacktestHarness.run(symbolLabel + " [" + thresholdLabel + "]", candles, thresholds);
        printCompact(report);
        assertStructurallySane(report);
    }

    private void printCompact(BacktestReport report) {
        System.out.println();
        System.out.println(report.label() + ":");
        for (SignalRuleId ruleId : DIRECTIONAL_RULES) {
            printCheckpointLine("  " + ruleId, report.directionalStats().get(ruleId));
        }
        printCheckpointLine("  Overall BUY ", report.overallBuy());
        printCheckpointLine("  Overall SELL", report.overallSell());
    }

    private void printCheckpointLine(String rowLabel, DirectionalOutcomeStats stats) {
        if (stats == null || stats.totalCalls() == 0) {
            System.out.printf("%-24s (n=0)%n", rowLabel);
            return;
        }
        System.out.printf("%-24s min %5.1f%%win exp%+6.3f%%(n=%-3d) | mid %5.1f%%win exp%+6.3f%%(n=%-3d) | max %5.1f%%win exp%+6.3f%%(n=%-3d)%n",
                rowLabel,
                stats.min().winRate(), stats.min().expectancyPct(), stats.min().scored(),
                stats.mid().winRate(), stats.mid().expectancyPct(), stats.mid().scored(),
                stats.max().winRate(), stats.max().expectancyPct(), stats.max().scored());
    }

    private void printIndicatorLine(String label, CheckpointStats stats) {
        System.out.printf("%s: %5.1f%% win (%d scored, %d n) | avg win %+6.2f%% | avg loss %+6.2f%% | expectancy %+6.3f%% (after costs %+6.3f%%)%n",
                label, stats.winRate(), stats.scored(), stats.scored() + stats.notScored(), stats.avgWinReturnPct(),
                stats.avgLossReturnPct(), stats.expectancyPct(), stats.expectancyPctAfterCosts());
    }

    /** Same structural invariants {@code ThresholdCalibrationTest}/{@link BacktestHarnessTest}
     * already check against the baseline (E2-F4-S1/S2), reapplied per held-out/untouched-fixture
     * run since a different candle slice changes bucket membership. */
    private void assertStructurallySane(BacktestReport report) {
        int totalFromCounts = report.callCounts().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(report.totalDecisionPoints(), totalFromCounts,
                report.label() + ": every decision point must land in exactly one SignalRuleId bucket");

        for (SignalRuleId ruleId : SignalRuleId.values()) {
            int expected = report.callCounts().get(ruleId);
            if (ruleId.call() == SignalCall.BUY || ruleId.call() == SignalCall.SELL) {
                DirectionalOutcomeStats stats = report.directionalStats().get(ruleId);
                assertEquals(expected, stats.totalCalls(),
                        report.label() + " " + ruleId + ": directional stats total must match its call count");
                assertExpectancySignsAreSane(report.label() + " " + ruleId, stats);
            } else {
                assertEquals(expected, report.holdGateStats().get(ruleId).totalCalls(),
                        report.label() + " " + ruleId + ": hold-gate stats total must match its call count");
            }
        }
        assertExpectancySignsAreSane(report.label() + " Overall BUY", report.overallBuy());
        assertExpectancySignsAreSane(report.label() + " Overall SELL", report.overallSell());
    }

    private void assertExpectancySignsAreSane(String label, DirectionalOutcomeStats stats) {
        for (Checkpoint checkpoint : Checkpoint.values()) {
            CheckpointStats cp = checkpoint == Checkpoint.MIN ? stats.min()
                    : checkpoint == Checkpoint.MID ? stats.mid() : stats.max();
            assertIndicatorStatsSane(label + " " + checkpoint, cp);
        }
    }

    /** Same structural invariants {@code IndicatorExpectancyCalibrationTest} already checks for
     * per-indicator single-checkpoint stats, reused here for both the directional-rule checkpoints
     * and the per-indicator stats since both are {@link CheckpointStats}. */
    private void assertIndicatorStatsSane(String label, CheckpointStats stats) {
        if (stats.win() > 0) {
            assertTrue(stats.avgWinReturnPct() > 0, label + ": avg win size must be positive");
        }
        if (stats.loss() > 0) {
            assertTrue(stats.avgLossReturnPct() < 0, label + ": avg loss size must be negative");
        }
        assertEquals(stats.scored(), stats.tpHit() + stats.slHit() + stats.horizonExpired(),
                label + ": tpHit+slHit+horizonExpired must partition scored()");
        assertTrue(stats.expectancyPctAfterCosts() <= stats.expectancyPct(),
                label + ": after-cost expectancy must never exceed raw expectancy");
    }
}
