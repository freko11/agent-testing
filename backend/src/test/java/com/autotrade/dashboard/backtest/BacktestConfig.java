package com.autotrade.dashboard.backtest;

import java.math.BigDecimal;

/**
 * Harness-only diagnostic thresholds used to measure backtest outcomes (E2-F4-S1) —
 * deliberately separate from and NOT versioned with
 * {@link com.autotrade.dashboard.signal.SignalRuleEngine#RULE_TABLE_VERSION} or
 * {@link com.autotrade.dashboard.signal.HoldTermCalculator#HOLD_TERM_TABLE_VERSION}, since these
 * measure outcomes rather than define the rule table under test.
 */
public final class BacktestConfig {

    /** A forward move smaller than this (in either direction, relative to the called direction)
     * counts as a WASH rather than a WIN or LOSS. */
    public static final BigDecimal WIN_LOSS_DEADBAND_PCT = new BigDecimal("0.25");

    /** Fixed reference horizon (in candle-index steps) used to score every HOLD call, regardless
     * of which rule matched — HOLD calls carry no hold-term of their own to derive a horizon from. */
    public static final int HOLD_REFERENCE_HORIZON_DAYS = 5;

    /** A forward move (absolute value) beyond this over the reference horizon counts as "large". */
    public static final BigDecimal LARGE_MOVE_THRESHOLD_PCT = new BigDecimal("3.0");

    private BacktestConfig() {
    }
}
