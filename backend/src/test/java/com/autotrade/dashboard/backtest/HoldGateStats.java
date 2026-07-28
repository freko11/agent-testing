package com.autotrade.dashboard.backtest;

/**
 * Backtest outcome for one HOLD-producing {@link com.autotrade.dashboard.signal.SignalRuleId}:
 * what fraction of the time a {@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}-day forward
 * move actually turned out "large" ({@link BacktestConfig#LARGE_MOVE_THRESHOLD_PCT}) — evidence
 * for whether suppressing a call under this rule was actually warranted.
 */
public record HoldGateStats(int totalCalls, int scoredCount, int largeMoveCount) {

    public double largeMoveRate() {
        return scoredCount == 0 ? 0.0 : (100.0 * largeMoveCount / scoredCount);
    }
}
