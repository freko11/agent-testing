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
    PARTIALLY_PROTECTED,
    /**
     * {@code placeOrder} threw {@code BrokerAdapterAmbiguousOrderException}
     * (E5-F2-S1) — genuinely unknown whether the order reached the broker,
     * since both the original call and the reconciliation probe failed.
     * Deliberately distinct from {@link #FAILED} (which means the broker
     * confirmed the order was never submitted, safe to retry): resubmitting
     * under a new identity while this status is unresolved risks a real
     * duplicate position. Resolve by retrying with the SAME clientOrderId
     * once the broker is reachable, or by checking the broker's own account
     * view directly.
     */
    SUBMISSION_UNKNOWN
}
