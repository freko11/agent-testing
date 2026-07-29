package com.autotrade.dashboard.order;

public enum OrderStatus {
    PENDING,
    SUBMITTED,
    FILLED,
    PARTIALLY_FILLED,
    REJECTED,
    CANCELLED,
    FAILED,
    /**
     * The entry leg of a multi-order bracket filled, but one or both of its
     * take-profit/stop-loss legs could not be placed (E4-F3-S2) — a real,
     * open leveraged position exists with incomplete protection. Only
     * {@link com.autotrade.dashboard.brokeradapter.BinanceFuturesTradingAdapter}
     * produces this today, since Alpaca's native single-call bracket order
     * has no equivalent partial-failure mode.
     */
    PARTIALLY_PROTECTED
}
