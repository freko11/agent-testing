package com.autotrade.dashboard.signal;

/**
 * The three indicators {@link SignalRuleEngine} votes across (E8-F3-S1) — used to key
 * per-indicator backtested expectancy (in {@code BacktestHarness}) and per-indicator weights
 * (in {@link WeightedVoteRuleEngine}), independent of any specific rule table version.
 */
public enum IndicatorId {
    RSI,
    MACD,
    MA_CROSSOVER
}
