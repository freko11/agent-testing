package com.autotrade.dashboard.backtest;

/**
 * How a BUY/SELL call's forward price move classifies at a given {@link Checkpoint}, against
 * {@link BacktestConfig#WIN_LOSS_DEADBAND_PCT}.
 */
public enum DirectionalOutcome {
    WIN,
    LOSS,
    WASH
}
