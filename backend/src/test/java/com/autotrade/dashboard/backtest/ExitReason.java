package com.autotrade.dashboard.backtest;

/**
 * How a BUY/SELL decision point's {@link Checkpoint} was actually resolved (E8-F2-S1): a
 * take-profit/stop-loss price crossed during the day-by-day walk-forward scan, or neither
 * crossed and the checkpoint fell back to the existing fixed-day endpoint scoring.
 */
public enum ExitReason {
    TP_HIT,
    SL_HIT,
    HORIZON_EXPIRED
}
