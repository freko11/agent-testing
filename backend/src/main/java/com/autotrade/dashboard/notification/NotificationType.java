package com.autotrade.dashboard.notification;

/**
 * What a {@link Notification} is about. The {@code ORDER_*} values mirror
 * every status {@code OrderService.applyOutcome} can ever persist onto an
 * {@code Order} — not just the AC's literally-named "fills or is rejected" —
 * since {@code PARTIALLY_PROTECTED}/{@code SUBMISSION_UNKNOWN} are exactly
 * the states E4/E5's own design gates flagged as needing manual attention,
 * and {@code CANCELLED} is reachable via a manual {@code refreshOrder} re-poll
 * (e.g. Binance's composite status mapping, E4-F3-S2). {@code SIGNAL_CHANGED}
 * is the watchlist half of the AC (E3-F3-S1).
 */
public enum NotificationType {
    ORDER_FILLED,
    ORDER_PARTIALLY_FILLED,
    ORDER_REJECTED,
    ORDER_FAILED,
    ORDER_CANCELLED,
    ORDER_PARTIALLY_PROTECTED,
    ORDER_SUBMISSION_UNKNOWN,
    SIGNAL_CHANGED
}
