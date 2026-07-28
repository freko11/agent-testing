package com.autotrade.dashboard.signal;

import java.math.BigDecimal;

/**
 * Derives a suggested hold-term (E2-F3-S2) from a matched {@link SignalRuleId} and the
 * ticker's ATR%volatility. Pure static, mirroring {@link SignalRuleEngine}'s shape.
 *
 * <p>Only rules whose {@link SignalRuleId#call()} is BUY or SELL get a hold-term — see
 * {@link #calculate}'s null contract. {@link #HOLD_TERM_TABLE_VERSION} is versioned
 * independently of {@link SignalRuleEngine#RULE_TABLE_VERSION}: a revision to the day-range
 * table shouldn't force reinterpreting historical BUY/SELL calls and vice versa.
 */
public final class HoldTermCalculator {

    public static final String HOLD_TERM_TABLE_VERSION = "v1";

    public static final BigDecimal VOLATILITY_LOW_MAX = new BigDecimal("2.0");
    public static final BigDecimal VOLATILITY_MEDIUM_MAX = new BigDecimal("5.0");

    private HoldTermCalculator() {
    }

    /**
     * @return the suggested hold-term, or {@code null} if {@code matchedRule} is a HOLD
     * (no entry to size a horizon for).
     */
    public static HoldTerm calculate(SignalRuleId matchedRule, BigDecimal volatility) {
        if (matchedRule.call() != SignalCall.BUY && matchedRule.call() != SignalCall.SELL) {
            return null;
        }

        TrendStrength trendStrength = switch (matchedRule) {
            case BULLISH_UNANIMOUS, BEARISH_UNANIMOUS -> TrendStrength.STRONG;
            case BULLISH_MAJORITY, BEARISH_MAJORITY -> TrendStrength.MODERATE;
            default -> throw new IllegalStateException(
                    "Unexpected BUY/SELL rule with no TrendStrength mapping: " + matchedRule);
        };

        VolatilityBand volatilityBand = classifyVolatility(volatility);

        HoldTermRule rule = HoldTermRule.match(trendStrength, volatilityBand);
        return HoldTerm.of(rule, HOLD_TERM_TABLE_VERSION);
    }

    private static VolatilityBand classifyVolatility(BigDecimal volatility) {
        if (volatility.compareTo(VOLATILITY_LOW_MAX) < 0) {
            return VolatilityBand.LOW;
        }
        if (volatility.compareTo(VOLATILITY_MEDIUM_MAX) < 0) {
            return VolatilityBand.MEDIUM;
        }
        return VolatilityBand.HIGH;
    }
}
