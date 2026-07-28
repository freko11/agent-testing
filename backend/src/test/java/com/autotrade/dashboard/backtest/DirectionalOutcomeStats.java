package com.autotrade.dashboard.backtest;

/**
 * Backtest outcome for one BUY/SELL {@link com.autotrade.dashboard.signal.SignalRuleId}, scored
 * at all three {@link Checkpoint}s so the hold-term day range itself (not just the call's
 * direction) can be evaluated — see {@link BacktestReport#printTo} for how min/mid/max win
 * rates are compared.
 */
public record DirectionalOutcomeStats(int totalCalls, CheckpointStats min, CheckpointStats mid, CheckpointStats max) {
}
