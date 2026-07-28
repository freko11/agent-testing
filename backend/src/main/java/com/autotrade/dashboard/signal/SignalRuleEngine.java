package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;

import java.math.BigDecimal;

/**
 * Deterministic Buy/Sell/Hold rule table (E2-F3-S1): three safety gates (null/dried-up
 * volume, extreme volatility) run first and can only ever force HOLD, then a 2-of-3
 * directional vote across RSI/MACD/MA-crossover with no dissent allowed decides BUY/SELL.
 * Rules are evaluated in {@link SignalRuleId} declaration order; the first match wins.
 *
 * <p>RSI 30/70 are the conventional overbought/oversold thresholds. The two gate
 * thresholds ({@link #VOLATILITY_EXTREME_THRESHOLD}, {@link #VOLUME_DRIED_UP_THRESHOLD})
 * are provisional engineering estimates, not yet backtest-validated — revisit once
 * E2-F4-S1's backtest harness exists. {@link #RULE_TABLE_VERSION} exists so revising
 * these thresholds later is an auditable, versioned change (feeds E6-F3-S2).
 */
public final class SignalRuleEngine {

    public static final String RULE_TABLE_VERSION = "v1";

    public static final BigDecimal RSI_OVERSOLD_THRESHOLD = new BigDecimal("30");
    public static final BigDecimal RSI_OVERBOUGHT_THRESHOLD = new BigDecimal("70");
    public static final BigDecimal VOLATILITY_EXTREME_THRESHOLD = new BigDecimal("8.0");
    public static final BigDecimal VOLUME_DRIED_UP_THRESHOLD = new BigDecimal("0.20");

    private SignalRuleEngine() {
    }

    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend) {
        if (volumeTrend == null) {
            return SignalRuleId.NO_VOLUME_DATA;
        }
        if (volumeTrend.compareTo(VOLUME_DRIED_UP_THRESHOLD) < 0) {
            return SignalRuleId.VOLUME_DRIED_UP;
        }
        if (volatility.compareTo(VOLATILITY_EXTREME_THRESHOLD) > 0) {
            return SignalRuleId.VOLATILITY_TOO_EXTREME;
        }

        boolean rsiBullish = rsi.compareTo(RSI_OVERSOLD_THRESHOLD) < 0;
        boolean rsiBearish = rsi.compareTo(RSI_OVERBOUGHT_THRESHOLD) > 0;
        boolean macdBullish = macd.histogram().signum() > 0;
        boolean macdBearish = macd.histogram().signum() < 0;
        boolean maBullish = movingAverage.relation() == MovingAverageRelation.SHORT_ABOVE_LONG;
        boolean maBearish = movingAverage.relation() == MovingAverageRelation.SHORT_BELOW_LONG;

        int bullishCount = count(rsiBullish, macdBullish, maBullish);
        int bearishCount = count(rsiBearish, macdBearish, maBearish);

        if (bullishCount > 0 && bearishCount > 0) {
            return SignalRuleId.CONFLICTING_SIGNALS;
        }
        if (bullishCount == 3) {
            return SignalRuleId.BULLISH_UNANIMOUS;
        }
        if (bullishCount == 2) {
            return SignalRuleId.BULLISH_MAJORITY;
        }
        if (bearishCount == 3) {
            return SignalRuleId.BEARISH_UNANIMOUS;
        }
        if (bearishCount == 2) {
            return SignalRuleId.BEARISH_MAJORITY;
        }
        return SignalRuleId.NO_STRONG_SIGNAL;
    }

    private static int count(boolean a, boolean b, boolean c) {
        return (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
    }
}
