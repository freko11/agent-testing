package com.autotrade.dashboard.backtest;

/**
 * The three forward-looking horizons a BUY/SELL call's hold-term implies, used to score
 * directional win/loss (E2-F4-S1). MIN/MAX bracket the suggested hold-term range itself
 * ({@link com.autotrade.dashboard.signal.HoldTerm#minDays()}/{@code #maxDays()}), MID is
 * their rounded midpoint. Scoring all three (not just one) is what lets the hold-term range
 * itself be evaluated, not just the call's direction.
 */
public enum Checkpoint {
    MIN,
    MID,
    MAX
}
