package com.autotrade.dashboard.backtest;

/**
 * How a BUY/SELL call's forward price move classifies at a given {@link Checkpoint}, against
 * {@link BacktestConfig#WIN_LOSS_DEADBAND_PCT}.
 *
 * <p>Promoted to main scope (E8-F5-S1) — see {@link BacktestConfig}'s class Javadoc.
 */
public enum DirectionalOutcome {
    WIN,
    LOSS,
    WASH
}
