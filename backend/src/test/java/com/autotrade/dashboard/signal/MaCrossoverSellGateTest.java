package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F1-S11: covers {@link MaCrossoverSellGate#applySellGate}/{@link
 * MaCrossoverSellGate#sellGateAppliesTo} directly, mirroring {@code RegimeGatedRuleEngineTest}'s
 * style for {@link RegimeGatedRuleEngine#applySellGate}. Unlike that gate (whose {@link Regime}
 * input is orthogonal to vote computation), this one re-derives the SELL classification from the
 * raw indicator inputs under a stricter threshold, so these cases construct real {@link MacdResult}/
 * {@link MovingAverageResult} fixtures rather than pure enum-in/enum-out ones.
 */
class MaCrossoverSellGateTest {

    private static final BigDecimal RSI_NEUTRAL = new BigDecimal("50");
    private static final BigDecimal RSI_BEARISH = new BigDecimal("80");
    private static final BigDecimal VOLATILITY_NORMAL = new BigDecimal("2.0");
    private static final BigDecimal VOLUME_TREND_NORMAL = new BigDecimal("1.0");

    private static final MacdResult MACD_NEUTRAL = macd("0", "0");
    private static final MacdResult MACD_BEARISH = macd("-1.0", "-1.0");

    private static final SignalRuleEngine.RuleThresholds BASE = SignalRuleEngine.RuleThresholds.DEFAULT;

    /**
     * RSI + MA bearish (MACD neutral) → BEARISH_MAJORITY under the base (separation=0) threshold.
     * Separation is 1.00%, short of {@link MaCrossoverSellGate#SELL_MIN_SEPARATION_PCT_OF_PRICE}
     * (2.00%), so the re-evaluation drops the MA vote entirely, leaving only RSI's lone bearish
     * vote — below the 2-of-3 majority bar, so the gate collapses the call to NO_STRONG_SIGNAL.
     */
    @Test
    void sellWithInsufficientSeparation_collapsesToNoStrongSignal() {
        MovingAverageResult ma = ma(MovingAverageRelation.SHORT_BELOW_LONG, "1.00");
        SignalRuleId matchedRule = SignalRuleEngine.evaluate(RSI_BEARISH, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.BEARISH_MAJORITY, matchedRule, "sanity check: base evaluation must be a SELL call");

        SignalRuleId gated = MaCrossoverSellGate.applySellGate(matchedRule, RSI_BEARISH, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, gated);
    }

    /**
     * Same shape as above, but separation is 3.00% — clears the 2.00% bar, so the MA vote still
     * counts under the stricter re-evaluation and the call stays BEARISH_MAJORITY, unchanged.
     */
    @Test
    void sellWithSufficientSeparation_unchanged() {
        MovingAverageResult ma = ma(MovingAverageRelation.SHORT_BELOW_LONG, "3.00");
        SignalRuleId matchedRule = SignalRuleEngine.evaluate(RSI_BEARISH, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.BEARISH_MAJORITY, matchedRule, "sanity check: base evaluation must be a SELL call");

        SignalRuleId gated = MaCrossoverSellGate.applySellGate(matchedRule, RSI_BEARISH, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.BEARISH_MAJORITY, gated);
    }

    /**
     * RSI + MACD + MA all bearish → BEARISH_UNANIMOUS under the base threshold. With insufficient
     * MA separation (1.00%), the re-evaluation drops the MA vote, leaving RSI + MACD — still a
     * 2-of-3 majority, so the call downgrades to BEARISH_MAJORITY (still a SELL call) rather than
     * collapsing to NO_STRONG_SIGNAL — the gate only ever suppresses a call that stops being SELL
     * entirely, not one that merely loses UNANIMOUS status.
     */
    @Test
    void unanimousSellWithInsufficientSeparation_downgradesToMajorityNotSuppressed() {
        MovingAverageResult ma = ma(MovingAverageRelation.SHORT_BELOW_LONG, "1.00");
        SignalRuleId matchedRule = SignalRuleEngine.evaluate(RSI_BEARISH, MACD_BEARISH, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.BEARISH_UNANIMOUS, matchedRule, "sanity check: base evaluation must be unanimous SELL");

        SignalRuleId gated = MaCrossoverSellGate.applySellGate(matchedRule, RSI_BEARISH, MACD_BEARISH, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.BEARISH_MAJORITY, gated);
    }

    @Test
    void buyCall_unaffectedRegardlessOfSeparation() {
        MovingAverageResult ma = ma(MovingAverageRelation.SHORT_ABOVE_LONG, "0.00");
        SignalRuleId gated = MaCrossoverSellGate.applySellGate(SignalRuleId.BULLISH_UNANIMOUS, RSI_NEUTRAL,
                MACD_NEUTRAL, ma, VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE);
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS, gated);
    }

    @Test
    void holdCauseRule_unaffectedRegardlessOfSeparation() {
        MovingAverageResult ma = ma(MovingAverageRelation.EQUAL, "0.00");
        assertEquals(SignalRuleId.VOLATILITY_TOO_EXTREME, MaCrossoverSellGate.applySellGate(
                SignalRuleId.VOLATILITY_TOO_EXTREME, RSI_NEUTRAL, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE));
        assertEquals(SignalRuleId.CONFLICTING_SIGNALS, MaCrossoverSellGate.applySellGate(
                SignalRuleId.CONFLICTING_SIGNALS, RSI_NEUTRAL, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE));
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL, MaCrossoverSellGate.applySellGate(
                SignalRuleId.NO_STRONG_SIGNAL, RSI_NEUTRAL, MACD_NEUTRAL, ma,
                VOLATILITY_NORMAL, VOLUME_TREND_NORMAL, BASE));
    }

    @Test
    void sellGateAppliesTo_crypto_true() {
        assertTrue(MaCrossoverSellGate.sellGateAppliesTo(AssetType.CRYPTO));
    }

    @Test
    void sellGateAppliesTo_stock_false() {
        assertFalse(MaCrossoverSellGate.sellGateAppliesTo(AssetType.STOCK));
    }

    private static MacdResult macd(String histogram, String histogramPctOfPrice) {
        return new MacdResult(new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal(histogram),
                new BigDecimal(histogramPctOfPrice).abs());
    }

    private static MovingAverageResult ma(MovingAverageRelation relation, String separationPctOfPrice) {
        return new MovingAverageResult(10, new BigDecimal("100"), 30, new BigDecimal("100"), relation,
                new BigDecimal(separationPctOfPrice));
    }
}
