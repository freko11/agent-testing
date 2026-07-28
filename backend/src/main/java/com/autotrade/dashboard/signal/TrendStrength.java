package com.autotrade.dashboard.signal;

/**
 * How strongly {@link SignalRuleEngine}'s directional vote agreed on a BUY/SELL call —
 * derived from the matched {@link SignalRuleId} itself (UNANIMOUS vs. MAJORITY), not
 * recomputed from raw indicator values. Feeds {@link HoldTermCalculator}.
 */
public enum TrendStrength {
    STRONG,
    MODERATE
}
