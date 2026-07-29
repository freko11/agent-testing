package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The outcome of a {@link BrokerAdapter} placeOrder/getOrderStatus/cancelOrder
 * call. {@code filledPrice} is non-null only when {@code status} is {@code
 * FILLED}, {@code PARTIALLY_FILLED}, or {@code PARTIALLY_PROTECTED} (the
 * entry itself still filled at that price); {@code rejectionReason} is
 * non-null when {@code status} is {@code REJECTED}, {@code FAILED}, or
 * {@code PARTIALLY_PROTECTED} (E4-F3-S2) — for the latter it describes which
 * take-profit/stop-loss leg is missing and why, since the position is real
 * and open but not fully protected.
 */
public record BrokerOrderResult(
        String clientOrderId,
        String brokerOrderId,
        OrderStatus status,
        BigDecimal filledPrice,
        String rejectionReason,
        Instant asOf) {
}
