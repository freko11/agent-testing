package com.autotrade.dashboard.signal;

/**
 * The 6-branch hold-term day-range table (E2-F3-S2): {@link TrendStrength} x
 * {@link VolatilityBand} cross-product, mirroring {@link SignalRuleId}'s shape as a
 * documented, versioned, per-branch-testable table. Ranges here are provisional
 * engineering estimates, not yet backtest-validated — revisit once E2-F4-S1's backtest
 * harness exists and can check realized hold-term accuracy, not just call win/loss.
 */
public enum HoldTermRule {

    STRONG_LOW(TrendStrength.STRONG, VolatilityBand.LOW, 5, 15,
            "Strong directional agreement with low volatility supports a longer hold before the trend plays out."),
    STRONG_MEDIUM(TrendStrength.STRONG, VolatilityBand.MEDIUM, 3, 10,
            "Strong directional agreement with moderate volatility supports a multi-day hold."),
    STRONG_HIGH(TrendStrength.STRONG, VolatilityBand.HIGH, 2, 6,
            "Strong directional agreement, but high volatility argues for a shorter hold to limit whipsaw risk."),
    MODERATE_LOW(TrendStrength.MODERATE, VolatilityBand.LOW, 3, 10,
            "Majority (not unanimous) agreement with low volatility supports a moderate hold."),
    MODERATE_MEDIUM(TrendStrength.MODERATE, VolatilityBand.MEDIUM, 2, 7,
            "Majority agreement with moderate volatility supports a shorter multi-day hold."),
    MODERATE_HIGH(TrendStrength.MODERATE, VolatilityBand.HIGH, 1, 4,
            "Majority agreement with high volatility argues for a short hold to limit whipsaw risk.");

    private final TrendStrength trendStrength;
    private final VolatilityBand volatilityBand;
    private final int minDays;
    private final int maxDays;
    private final String rationale;

    HoldTermRule(TrendStrength trendStrength, VolatilityBand volatilityBand, int minDays, int maxDays, String rationale) {
        this.trendStrength = trendStrength;
        this.volatilityBand = volatilityBand;
        this.minDays = minDays;
        this.maxDays = maxDays;
        this.rationale = rationale;
    }

    public static HoldTermRule match(TrendStrength trendStrength, VolatilityBand volatilityBand) {
        for (HoldTermRule rule : values()) {
            if (rule.trendStrength == trendStrength && rule.volatilityBand == volatilityBand) {
                return rule;
            }
        }
        throw new IllegalStateException("No HoldTermRule for " + trendStrength + "/" + volatilityBand);
    }

    public int minDays() {
        return minDays;
    }

    public int maxDays() {
        return maxDays;
    }

    public String rationale() {
        return rationale;
    }
}
