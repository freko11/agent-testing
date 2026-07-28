package com.autotrade.dashboard.signal;

/**
 * A coarse classification of ATR%volatility for hold-term purposes. Threshold constants
 * live on {@link HoldTermCalculator}, not here, matching {@link SignalRuleEngine} keeping
 * its own threshold constants rather than pushing them into a helper enum.
 */
public enum VolatilityBand {
    LOW,
    MEDIUM,
    HIGH
}
