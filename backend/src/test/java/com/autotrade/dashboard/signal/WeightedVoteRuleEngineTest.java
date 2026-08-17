package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine.IndicatorWeights;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E8-F3-S1: mirrors {@link SignalRuleEngineTest}'s per-branch coverage style, plus the weighted
 * vote's two new behaviors this story exists to add — a dominant lone/2-of-3 indicator promoting
 * to a directional call, and a low-weight one still resolving NO_STRONG_SIGNAL (proving the
 * threshold actually gates, not just that weighting exists).
 */
class WeightedVoteRuleEngineTest {

    private static final BigDecimal RSI_NEUTRAL = new BigDecimal("50");
    private static final BigDecimal RSI_OVERSOLD = new BigDecimal("20");
    private static final BigDecimal RSI_OVERBOUGHT = new BigDecimal("80");

    private static final MacdResult MACD_NEUTRAL = macd("0");
    private static final MacdResult MACD_BULLISH = macd("1.0");
    private static final MacdResult MACD_BEARISH = macd("-1.0");

    private static final MovingAverageResult MA_NEUTRAL = ma(MovingAverageRelation.EQUAL);
    private static final MovingAverageResult MA_BULLISH = ma(MovingAverageRelation.SHORT_ABOVE_LONG);
    private static final MovingAverageResult MA_BEARISH = ma(MovingAverageRelation.SHORT_BELOW_LONG);

    private static final BigDecimal VOLATILITY_NORMAL = new BigDecimal("2.0");
    private static final BigDecimal VOLUME_TREND_NORMAL = new BigDecimal("1.0");

    private static final IndicatorWeights EQUAL_WEIGHTS =
            new IndicatorWeights(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("1"));

    /** RSI alone carries well over half the total weight — a dominant indicator. */
    private static final IndicatorWeights RSI_DOMINANT =
            new IndicatorWeights(new BigDecimal("10"), new BigDecimal("1"), new BigDecimal("1"));

    /** RSI alone carries well under half the total weight — a weak/low-weight indicator. */
    private static final IndicatorWeights RSI_WEAK =
            new IndicatorWeights(new BigDecimal("1"), new BigDecimal("10"), new BigDecimal("10"));

    @Test
    void nullVolumeTrend_returnsNoVolumeData() {
        assertEquals(SignalRuleId.NO_VOLUME_DATA, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH,
                MA_BULLISH, VOLATILITY_NORMAL, null, SignalRuleEngine.RuleThresholds.DEFAULT, EQUAL_WEIGHTS));
    }

    @Test
    void volumeTrendBelowThreshold_returnsVolumeDriedUp() {
        assertEquals(SignalRuleId.VOLUME_DRIED_UP, WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL,
                MA_NEUTRAL, VOLATILITY_NORMAL, new BigDecimal("0.15"), SignalRuleEngine.RuleThresholds.DEFAULT,
                EQUAL_WEIGHTS));
    }

    @Test
    void volatilityAboveThreshold_returnsVolatilityTooExtreme() {
        assertEquals(SignalRuleId.VOLATILITY_TOO_EXTREME, WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL,
                MA_NEUTRAL, new BigDecimal("8.5"), VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                EQUAL_WEIGHTS));
    }

    @Test
    void conflictingSignals_bullishAndBearishMix_returnsConflictingSignals() {
        // RSI dominant weight still can't override dissent: the conflict gate runs on raw votes.
        assertEquals(SignalRuleId.CONFLICTING_SIGNALS, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_BEARISH,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_DOMINANT));
    }

    @Test
    void allThreeBullish_returnsBullishUnanimous_regardlessOfWeights() {
        // Even maximally lopsided weights can't change this branch, since it's decided by the
        // raw 3-of-3 vote count, not a weight comparison.
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH,
                MA_BULLISH, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_WEAK));
    }

    @Test
    void allThreeBullish_returnsBullishUnanimous_evenWithZeroWeights() {
        IndicatorWeights zeroWeights = new IndicatorWeights(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH,
                MA_BULLISH, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                zeroWeights));
    }

    @Test
    void allThreeBearish_returnsBearishUnanimous_regardlessOfWeights() {
        assertEquals(SignalRuleId.BEARISH_UNANIMOUS, WeightedVoteRuleEngine.evaluate(RSI_OVERBOUGHT, MACD_BEARISH,
                MA_BEARISH, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_WEAK));
    }

    @Test
    void twoOfThreeBullish_equalWeights_returnsBullishMajority() {
        // Matches the unweighted table's own 2-of-3 majority behavior when weights don't favor
        // any one indicator.
        assertEquals(SignalRuleId.BULLISH_MAJORITY, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                EQUAL_WEIGHTS));
    }

    /** The story's core intended behavior: a single dominant indicator resolves a directional
     * call where the unweighted table would call NO_STRONG_SIGNAL (only 1 of 3 voted). */
    @Test
    void loneDominantIndicator_promotesToBullishMajority() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL), "sanity check: unweighted table calls this NO_STRONG_SIGNAL");

        assertEquals(SignalRuleId.BULLISH_MAJORITY, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_DOMINANT));
    }

    @Test
    void loneDominantIndicator_bearish_promotesToBearishMajority() {
        assertEquals(SignalRuleId.BEARISH_MAJORITY, WeightedVoteRuleEngine.evaluate(RSI_OVERBOUGHT, MACD_NEUTRAL,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_DOMINANT));
    }

    /** Proves the threshold actually gates, not just that weighting exists: a lone indicator
     * whose own weight is well under half the total still resolves NO_STRONG_SIGNAL. */
    @Test
    void loneWeakIndicator_staysNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_WEAK));
    }

    @Test
    void allNeutral_returnsNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                RSI_DOMINANT));
    }

    /** With every weight floored to zero (an all-zero {@link IndicatorWeights}, not the current
     * real {@link IndicatorWeights#DEFAULT} — see its Javadoc, {@code macdWeight} is nonzero since
     * E8-F3-S5), a lone or 2-of-3 vote can never reach the weighted-majority bar — only the
     * raw-count UNANIMOUS branch still resolves a directional call. Guards against the {@code 0
     * >= 0} vacuous-comparison bug this case would hit without an explicit zero-total-weight
     * check. */
    @Test
    void zeroTotalWeight_loneIndicator_staysNoStrongSignal() {
        IndicatorWeights zeroWeights = new IndicatorWeights(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL,
                MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, SignalRuleEngine.RuleThresholds.DEFAULT,
                zeroWeights));
    }

    /** {@code IndicatorWeights.DEFAULT}'s {@code rsiWeight} is still 0.000 (E8-F3-S5 never found a
     * horizon where RSI's weight clears zero), so a lone RSI vote (MACD/MA neutral) under the real
     * DEFAULT weights still resolves NO_STRONG_SIGNAL exactly as it did before that story shipped
     * a nonzero {@code macdWeight} — this case doesn't exercise MACD at all, so it's unaffected by
     * that change. */
    @Test
    void defaultEvaluate_usesDefaultWeights() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    /** E8-F3-S5's shipped change: {@code IndicatorWeights.DEFAULT.macdWeight()} is now the only
     * nonzero weight, so it alone equals {@code totalWeight} — a lone MACD vote (RSI/MA neutral)
     * now clears the weighted-majority bar under the real DEFAULT weights, where it stayed
     * NO_STRONG_SIGNAL before this story. */
    @Test
    void defaultEvaluate_loneMacdVote_promotesToBullishMajority() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_BULLISH, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL), "sanity check: unweighted table calls this NO_STRONG_SIGNAL");

        assertEquals(SignalRuleId.BULLISH_MAJORITY,
                WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_BULLISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    /** {@link WeightedVoteRuleEngine#evaluateUnweighted} must match {@link
     * SignalRuleEngine#evaluate} exactly across the same fixture inputs {@link
     * SignalRuleEngineTest} already uses — the literal "fallback/comparison mode" the AC asks
     * for. */
    @Test
    void evaluateUnweighted_matchesSignalRuleEngine_acrossAllBranches() {
        assertEqualsBothEngines(SignalRuleId.NO_VOLUME_DATA, RSI_OVERSOLD, MACD_BULLISH, MA_BULLISH,
                VOLATILITY_NORMAL, null);
        assertEqualsBothEngines(SignalRuleId.VOLUME_DRIED_UP, RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL,
                VOLATILITY_NORMAL, new BigDecimal("0.15"));
        assertEqualsBothEngines(SignalRuleId.VOLATILITY_TOO_EXTREME, RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL,
                new BigDecimal("8.5"), VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.BULLISH_UNANIMOUS, RSI_OVERSOLD, MACD_BULLISH, MA_BULLISH,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.BULLISH_MAJORITY, RSI_OVERSOLD, MACD_BULLISH, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.BEARISH_UNANIMOUS, RSI_OVERBOUGHT, MACD_BEARISH, MA_BEARISH,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.BEARISH_MAJORITY, RSI_OVERBOUGHT, MACD_BEARISH, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.CONFLICTING_SIGNALS, RSI_OVERSOLD, MACD_BEARISH, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.NO_STRONG_SIGNAL, RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
        assertEqualsBothEngines(SignalRuleId.NO_STRONG_SIGNAL, RSI_OVERSOLD, MACD_NEUTRAL, MA_NEUTRAL,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL);
    }

    /** E8-F3-S6's new 8-arg overload: passing {@code WEIGHTED_MAJORITY_FRACTION} explicitly must
     * match the 7-arg overload's own delegation to it exactly. */
    @Test
    void explicitDefaultFraction_matchesSevenArgOverload() {
        assertEquals(
                WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_BULLISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL,
                        SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT),
                WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_BULLISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL,
                        SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT,
                        WeightedVoteRuleEngine.WEIGHTED_MAJORITY_FRACTION));
    }

    /** E8-F3-S6's most permissive regime ({@code majorityFraction == 0}): a lone vote from a
     * still-zero-weighted indicator (RSI, under {@code IndicatorWeights.DEFAULT}) promotes to
     * BULLISH_MAJORITY, unlike at the shipped 0.5 where it stays NO_STRONG_SIGNAL — proven here at
     * the unit level even though {@code WeightedMajorityFractionCalibrationTest} found this
     * composition (a lone vote with no MACD involvement) never actually occurs in the real
     * BTCUSDT/DOGEUSDT fixture data. */
    @Test
    void zeroFraction_loneZeroWeightedVote_promotesToBullishMajority() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL,
                        SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT),
                "sanity check: at the shipped 0.5, a lone RSI vote (RSI still zero-weighted) stays NO_STRONG_SIGNAL");

        assertEquals(SignalRuleId.BULLISH_MAJORITY,
                WeightedVoteRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL,
                        SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT, BigDecimal.ZERO));
    }

    /** E8-F3-S6's least permissive regime ({@code majorityFraction > 1}): even a lone MACD vote —
     * the one indicator with a real nonzero {@code IndicatorWeights.DEFAULT} weight, which clears
     * the bar at every fraction in {@code (0, 1]} including the shipped 0.5 — fails to clear a
     * threshold above 1, since {@code majorityThreshold} then exceeds the maximum achievable
     * weighted sum (0.714). */
    @Test
    void aboveOneFraction_loneMacdVote_staysNoStrongSignal() {
        assertEquals(SignalRuleId.BULLISH_MAJORITY,
                WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_BULLISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL),
                "sanity check: at the shipped 0.5, a lone MACD vote already promotes (E8-F3-S5)");

        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                WeightedVoteRuleEngine.evaluate(RSI_NEUTRAL, MACD_BULLISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL,
                        SignalRuleEngine.RuleThresholds.DEFAULT, IndicatorWeights.DEFAULT, new BigDecimal("1.01")));
    }

    private void assertEqualsBothEngines(SignalRuleId expected, BigDecimal rsi, MacdResult macd,
                                          MovingAverageResult ma, BigDecimal volatility, BigDecimal volumeTrend) {
        SignalRuleId fromUnweightedTable = SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend);
        SignalRuleId fromDelegation = WeightedVoteRuleEngine.evaluateUnweighted(rsi, macd, ma, volatility, volumeTrend);
        assertEquals(expected, fromUnweightedTable);
        assertEquals(fromUnweightedTable, fromDelegation, "evaluateUnweighted must match SignalRuleEngine.evaluate exactly");
    }

    private static MacdResult macd(String histogram) {
        return new MacdResult(new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal(histogram), BigDecimal.ZERO);
    }

    private static MovingAverageResult ma(MovingAverageRelation relation) {
        return new MovingAverageResult(10, new BigDecimal("100"), 30, new BigDecimal("100"), relation, BigDecimal.ZERO);
    }
}
