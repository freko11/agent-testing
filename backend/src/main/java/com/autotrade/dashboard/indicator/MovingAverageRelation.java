package com.autotrade.dashboard.indicator;

/** Current relation between the short and long moving averages — not a "just crossed" event; see {@link MovingAverageCrossoverCalculator}. */
public enum MovingAverageRelation {
    SHORT_ABOVE_LONG,
    SHORT_BELOW_LONG,
    EQUAL
}
