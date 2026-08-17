package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine.IndicatorWeights;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S6: sweeps {@code WeightedVoteRuleEngine.WEIGHTED_MAJORITY_FRACTION} — a constant its own
 * Javadoc flags as "not backtest-derived... a future story could sweep this the same way E8-F1-S1
 * swept RSI thresholds" — now that {@code IndicatorWeights.DEFAULT} carries a real nonzero weight
 * (E8-F3-S5, {@code macdWeight = 0.714}) and the constant is no longer structurally inert.
 *
 * <p><b>What sweeping this constant actually changes, worked out from {@link
 * WeightedVoteRuleEngine#evaluate(BigDecimal, com.autotrade.dashboard.indicator.MacdResult,
 * com.autotrade.dashboard.indicator.MovingAverageResult, BigDecimal, BigDecimal,
 * SignalRuleEngine.RuleThresholds, IndicatorWeights, BigDecimal)}'s own code, not assumed:</b> with
 * {@code IndicatorWeights.DEFAULT} (rsiWeight=0.000, macdWeight=0.714, maCrossoverWeight=0.000),
 * {@code totalWeight} is always exactly 0.714, entirely from MACD. A lone-or-2-of-3 vote's {@code
 * weightedSum} can only ever be one of two values: 0.714 (MACD voted in that direction, regardless
 * of whether RSI/MA also voted, since their own weight contributes nothing) or 0.000 (MACD did not
 * vote). Comparing those two achievable sums against {@code majorityThreshold = 0.714 *
 * majorityFraction} collapses the entire real-valued fraction range into exactly three behavioral
 * regimes, not a continuum:
 * <ul>
 *   <li><b>{@code majorityFraction == 0}:</b> {@code majorityThreshold = 0}, so both achievable sums
 *   (0.000 and 0.714) clear it — every lone-or-2-of-3 vote promotes to BULLISH_MAJORITY/
 *   BEARISH_MAJORITY, including ones driven entirely by RSI and/or MA-crossover (both still
 *   zero-weighted), the most permissive regime.</li>
 *   <li><b>{@code 0 < majorityFraction <= 1}:</b> {@code majorityThreshold} lands strictly between
 *   0.000 and 0.714 (or exactly at 0.714 when {@code majorityFraction == 1}, still cleared by
 *   {@code >=}) — only votes where MACD contributed clear the bar; an RSI-and/or-MA-only vote never
 *   does. Every value in this half-open interval, including the shipped default 0.5, produces
 *   byte-identical classification, since only two sums are ever achievable and one threshold
 *   partition separates them the same way everywhere in this range.</li>
 *   <li><b>{@code majorityFraction > 1}:</b> {@code majorityThreshold > 0.714}, so neither
 *   achievable sum ever clears it — no lone-or-2-of-3 vote can ever promote, regardless of which
 *   indicators voted. Only the raw-count UNANIMOUS branch (decided independently of weight) still
 *   resolves a directional call, the least permissive regime.</li>
 * </ul>
 * So a "sweep" of this constant is really a choice of one of three regimes, not a continuous
 * optimization — confirmed empirically below ({@link #midRangeFractionsProduceByteIdenticalReports})
 * before spending any effort computing expectancy at redundant candidates within {@code (0, 1]}.
 *
 * <p><b>Scoring methodology:</b> {@link BacktestHarness#runCombinedCallExpectancy} (new, this
 * story) scores the *combined* rule-table call (not a single indicator's own vote, unlike {@link
 * BacktestHarness#runIndicatorExpectancy}) at the same 15-day/TP15%/SL9% horizon {@code
 * IndicatorWeights.DEFAULT}'s {@code macdWeight} was itself calibrated at (E8-F3-S5) — using the
 * rule-derived hold term {@link BacktestHarness#run} normally uses would reintroduce the
 * short-horizon mismatch E8-F3-S1's original all-zero finding hit. Tuning phase uses the full,
 * untouched-by-split BTCUSDT/DOGEUSDT fixtures, the same "tuning fixtures" E8-F3-S1/E8-F3-S5 used
 * for the weight calibration itself (not the 70/30 {@link FixtureSplits} split, which is reserved
 * for out-of-sample validation of a tuning-window winner, per {@link
 * OutOfSampleValidationTest}/E8-F4-S1's own methodology).
 *
 * <p><b>Result: no ship, kept at 0.5.</b> {@code majorityFraction = 0.00} produced a report
 * byte-identical to {@code 0.50} on both fixtures (combined after-cost expectancy +0.921%, n=825
 * scored / 838 total) — not a coincidence of the math (in general {@code 0.00} is strictly more
 * permissive), but a real property of this data: MACD's histogram is essentially never exactly
 * zero, so every lone/2-of-3 vote that exists in either fixture already includes MACD (confirmed
 * directly with a throwaway probe — zero RSI-only or MA-only lone/2-of-3 votes across either
 * fixture's decision points). {@code majorityFraction = 1.50} produced zero scored calls on both
 * fixtures: BULLISH_UNANIMOUS/BEARISH_UNANIMOUS never fire anywhere in this data (the same finding
 * {@code WeightedVoteBacktestTest}/E8-F3-S1 already documented), so disabling majority resolution
 * entirely disables the engine's output here — an unusable, always-empty call population, not a
 * candidate worth validating out-of-sample. Net: nothing in the swept range beats the shipped
 * default (0.00 ties, everything above 1 is strictly worse), so {@code WEIGHTED_MAJORITY_FRACTION}
 * stays at 0.5 unchanged — no out-of-sample validation step was needed per this story's own ship
 * bar, since no tuning-window candidate clearly beat the default to validate in the first place.
 * See {@link #printExpectancyAcrossRegimes} for the full printed report and {@code
 * WeightedVoteRuleEngine}'s class Javadoc for the closing finding.
 */
class WeightedMajorityFractionCalibrationTest {

    private static final List<Candle> BTCUSDT = FixtureSplits.BTCUSDT;
    private static final List<Candle> DOGEUSDT = FixtureSplits.DOGEUSDT;

    private static final int HORIZON_DAYS = 15;
    private static final BigDecimal TAKE_PROFIT_PCT = new BigDecimal("15.0");
    private static final BigDecimal STOP_LOSS_PCT = new BigDecimal("9.0");

    private record FractionCandidate(String label, BigDecimal fraction) {
    }

    private static final List<FractionCandidate> REGIME_CANDIDATES = List.of(
            new FractionCandidate("0.00 (most permissive: any lone/2-of-3 vote promotes)", BigDecimal.ZERO),
            new FractionCandidate("0.50 (current default)", new BigDecimal("0.5")),
            new FractionCandidate("1.50 (least permissive: only UNANIMOUS ever promotes)", new BigDecimal("1.5")));

    @Test
    void printExpectancyAcrossRegimes() {
        System.out.println();
        System.out.println("########## E8-F3-S6: WEIGHTED_MAJORITY_FRACTION expectancy across its three behavioral regimes ##########");

        for (FractionCandidate candidate : REGIME_CANDIDATES) {
            System.out.printf("%n=== majorityFraction = %s ===%n", candidate.label());

            BacktestHarness.RuleEvaluator evaluator = (rsi, macd, ma, volatility, volumeTrend) ->
                    WeightedVoteRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend,
                            SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT, candidate.fraction());

            Map<SignalCall, CheckpointStats> btc = BacktestHarness.runCombinedCallExpectancy(BTCUSDT, evaluator,
                    SignalRuleEngine.RuleThresholds.DEFAULT, HORIZON_DAYS, TAKE_PROFIT_PCT, STOP_LOSS_PCT);
            Map<SignalCall, CheckpointStats> doge = BacktestHarness.runCombinedCallExpectancy(DOGEUSDT, evaluator,
                    SignalRuleEngine.RuleThresholds.DEFAULT, HORIZON_DAYS, TAKE_PROFIT_PCT, STOP_LOSS_PCT);

            printRegime(btc, doge);
        }
    }

    private void printRegime(Map<SignalCall, CheckpointStats> btc, Map<SignalCall, CheckpointStats> doge) {
        CheckpointStats btcBuy = btc.get(SignalCall.BUY);
        CheckpointStats btcSell = btc.get(SignalCall.SELL);
        CheckpointStats dogeBuy = doge.get(SignalCall.BUY);
        CheckpointStats dogeSell = doge.get(SignalCall.SELL);
        CheckpointStats combinedBuy = BacktestHarness.combineCheckpoint(btcBuy, dogeBuy);
        CheckpointStats combinedSell = BacktestHarness.combineCheckpoint(btcSell, dogeSell);
        CheckpointStats combinedAll = BacktestHarness.combineCheckpoint(combinedBuy, combinedSell);

        printLine("  BTCUSDT  BUY ", btcBuy);
        printLine("  BTCUSDT  SELL", btcSell);
        printLine("  DOGEUSDT BUY ", dogeBuy);
        printLine("  DOGEUSDT SELL", dogeSell);
        printLine("  COMBINED BUY ", combinedBuy);
        printLine("  COMBINED SELL", combinedSell);
        printLine("  COMBINED ALL ", combinedAll);

        assertStructurallySane("BTCUSDT BUY", btcBuy);
        assertStructurallySane("BTCUSDT SELL", btcSell);
        assertStructurallySane("DOGEUSDT BUY", dogeBuy);
        assertStructurallySane("DOGEUSDT SELL", dogeSell);
    }

    private void printLine(String label, CheckpointStats stats) {
        System.out.printf("%s: %5.1f%% win (%d scored, %d n) | avg win %+6.2f%% | avg loss %+6.2f%% | expectancy %+6.3f%% (after costs %+6.3f%%)%n",
                label, stats.winRate(), stats.scored(), stats.scored() + stats.notScored(), stats.avgWinReturnPct(),
                stats.avgLossReturnPct(), stats.expectancyPct(), stats.expectancyPctAfterCosts());
    }

    /**
     * Empirical confirmation of the class Javadoc's mathematical claim: every fraction in {@code
     * (0, 1]} produces byte-identical classification to the shipped default (0.5), since only two
     * weighted sums (0 and 0.714) are ever achievable under {@code IndicatorWeights.DEFAULT} and one
     * threshold partition separates them identically everywhere in that half-open interval. Checked
     * against real decision points (not synthetic fixtures), across both tuning fixtures, so this
     * isn't just trusted from the arithmetic alone.
     */
    @Test
    void midRangeFractionsProduceByteIdenticalReports() {
        List<BigDecimal> midRangeCandidates = List.of(
                new BigDecimal("0.10"), new BigDecimal("0.25"), new BigDecimal("0.50"),
                new BigDecimal("0.75"), new BigDecimal("0.90"), new BigDecimal("1.00"));

        for (List<Candle> candles : List.of(BTCUSDT, DOGEUSDT)) {
            Map<SignalCall, CheckpointStats> baseline = runAt(candles, new BigDecimal("0.5"));
            for (BigDecimal fraction : midRangeCandidates) {
                Map<SignalCall, CheckpointStats> underTest = runAt(candles, fraction);
                assertEquals(baseline, underTest,
                        "majorityFraction=" + fraction + " must match 0.5 exactly within (0, 1] (both resolve "
                                + "off the same two achievable weighted sums, 0 and 0.714)");
            }
        }
    }

    /** {@code majorityFraction > 1} must never promote a lone/2-of-3 vote, even one where MACD (the
     * only nonzero-weighted indicator) voted — {@code majorityThreshold} exceeds the maximum
     * achievable weighted sum (0.714) everywhere in this regime. */
    @Test
    void aboveOneNeverPromotesMajorityCall() {
        for (List<Candle> candles : List.of(BTCUSDT, DOGEUSDT)) {
            Map<SignalCall, CheckpointStats> aboveOne = runAt(candles, new BigDecimal("1.01"));
            Map<SignalCall, CheckpointStats> wayAboveOne = runAt(candles, new BigDecimal("5.00"));
            assertEquals(aboveOne, wayAboveOne,
                    "any majorityFraction > 1 must behave identically (majorityThreshold always exceeds "
                            + "the max achievable weighted sum of 0.714) - checked at 1.01 and 5.00");
        }
    }

    private Map<SignalCall, CheckpointStats> runAt(List<Candle> candles, BigDecimal fraction) {
        BacktestHarness.RuleEvaluator evaluator = (rsi, macd, ma, volatility, volumeTrend) ->
                WeightedVoteRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend,
                        SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT, fraction);
        return BacktestHarness.runCombinedCallExpectancy(candles, evaluator, SignalRuleEngine.RuleThresholds.DEFAULT,
                HORIZON_DAYS, TAKE_PROFIT_PCT, STOP_LOSS_PCT);
    }

    /** Same structural invariants every other E8 calibration test checks. */
    private void assertStructurallySane(String label, CheckpointStats stats) {
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
