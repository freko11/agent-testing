package com.autotrade.dashboard.signal;

/**
 * Trend-strength/regime classification (E8-F3-S2), derived from {@link
 * com.autotrade.dashboard.indicator.AdxCalculator}'s ADX reading via {@link RegimeClassifier}.
 * TRENDING markets are ones a directional RSI/MACD/MA-crossover vote should be trusted in;
 * RANGING (choppy) markets are ones the same vote means less in — the same crossover that marks a
 * real trend change in a TRENDING market is often just noise in a RANGING one. Feeds {@link
 * RegimeGatedRuleEngine}.
 */
public enum Regime {
    TRENDING,
    RANGING
}
