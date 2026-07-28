package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** One test per {@link SignalRuleId} branch, plus threshold boundaries and gate-priority ordering. */
class SignalRuleEngineTest {

    private static final BigDecimal RSI_NEUTRAL = new BigDecimal("50");
    private static final BigDecimal RSI_OVERSOLD = new BigDecimal("25");
    private static final BigDecimal RSI_OVERBOUGHT = new BigDecimal("75");

    private static final MacdResult MACD_NEUTRAL = macd("0");
    private static final MacdResult MACD_BULLISH = macd("1.0");
    private static final MacdResult MACD_BEARISH = macd("-1.0");

    private static final MovingAverageResult MA_NEUTRAL = ma(MovingAverageRelation.EQUAL);
    private static final MovingAverageResult MA_BULLISH = ma(MovingAverageRelation.SHORT_ABOVE_LONG);
    private static final MovingAverageResult MA_BEARISH = ma(MovingAverageRelation.SHORT_BELOW_LONG);

    private static final BigDecimal VOLATILITY_NORMAL = new BigDecimal("2.0");
    private static final BigDecimal VOLUME_TREND_NORMAL = new BigDecimal("1.0");

    @Test
    void nullVolumeTrend_returnsNoVolumeData() {
        assertEquals(SignalRuleId.NO_VOLUME_DATA,
                SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH, MA_BULLISH, VOLATILITY_NORMAL, null));
    }

    @Test
    void volumeTrendBelowThreshold_returnsVolumeDriedUp() {
        assertEquals(SignalRuleId.VOLUME_DRIED_UP,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, new BigDecimal("0.15")));
    }

    @Test
    void volumeTrendExactlyAtThreshold_notDriedUp() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, new BigDecimal("0.20")));
    }

    @Test
    void volatilityAboveThreshold_returnsVolatilityTooExtreme() {
        assertEquals(SignalRuleId.VOLATILITY_TOO_EXTREME,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL, new BigDecimal("8.5"), VOLUME_TREND_NORMAL));
    }

    @Test
    void volatilityExactlyAtThreshold_notExtreme() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL, new BigDecimal("8.0"), VOLUME_TREND_NORMAL));
    }

    @Test
    void allThreeBullish_returnsBullishUnanimous() {
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS,
                SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH, MA_BULLISH, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void twoOfThreeBullish_oneNeutral_returnsBullishMajority() {
        assertEquals(SignalRuleId.BULLISH_MAJORITY,
                SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void allThreeBearish_returnsBearishUnanimous() {
        assertEquals(SignalRuleId.BEARISH_UNANIMOUS,
                SignalRuleEngine.evaluate(RSI_OVERBOUGHT, MACD_BEARISH, MA_BEARISH, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void twoOfThreeBearish_oneNeutral_returnsBearishMajority() {
        assertEquals(SignalRuleId.BEARISH_MAJORITY,
                SignalRuleEngine.evaluate(RSI_OVERBOUGHT, MACD_BEARISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void oneIndicatorBullish_othersNeutral_returnsNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void allNeutral_returnsNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void conflictingSignals_bullishAndBearishMix_returnsConflictingSignals() {
        assertEquals(SignalRuleId.CONFLICTING_SIGNALS,
                SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_BEARISH, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void rsiExactlyAtOversoldThreshold_notOversold() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(new BigDecimal("30"), MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void rsiExactlyAtOverboughtThreshold_notOverbought() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(new BigDecimal("70"), MACD_NEUTRAL, MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void macdHistogramExactlyZero_isNeutral() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, macd("0.00000000"), MA_NEUTRAL, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL));
    }

    @Test
    void nullVolumeTakesPriorityOverExtremeVolatility() {
        assertEquals(SignalRuleId.NO_VOLUME_DATA,
                SignalRuleEngine.evaluate(RSI_NEUTRAL, MACD_NEUTRAL, MA_NEUTRAL, new BigDecimal("9.0"), null));
    }

    @Test
    void volumeDriedUpTakesPriorityOverBullishSignals() {
        assertEquals(SignalRuleId.VOLUME_DRIED_UP,
                SignalRuleEngine.evaluate(RSI_OVERSOLD, MACD_BULLISH, MA_BULLISH, VOLATILITY_NORMAL, new BigDecimal("0.10")));
    }

    private static MacdResult macd(String histogram) {
        return new MacdResult(new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal(histogram));
    }

    private static MovingAverageResult ma(MovingAverageRelation relation) {
        return new MovingAverageResult(10, new BigDecimal("100"), 30, new BigDecimal("100"), relation);
    }
}
