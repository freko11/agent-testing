package com.autotrade.dashboard.backtest;

/**
 * E8-F3-S2: one direction's (BUY or SELL) expectancy split by {@link
 * com.autotrade.dashboard.signal.Regime} at decision time — the evidence "does a regime filter
 * earn its keep" requires: trending-regime expectancy compared directly against ranging-regime
 * expectancy for the same direction, using the exact same {@link DirectionalOutcomeStats} shape
 * the unsplit overall BUY/SELL stats already use.
 */
public record RegimeSplitStats(DirectionalOutcomeStats trending, DirectionalOutcomeStats ranging) {
}
