package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The outcome of a {@link BrokerAdapter} placeOrder/getOrderStatus/cancelOrder
 * call. {@code filledPrice} is non-null only when {@code status} is {@code
 * FILLED} or {@code PARTIALLY_FILLED}; {@code rejectionReason} is non-null
 * only when {@code status} is {@code REJECTED} or {@code FAILED}.
 */
public record BrokerOrderResult(
        String clientOrderId,
        String brokerOrderId,
        OrderStatus status,
        BigDecimal filledPrice,
        String rejectionReason,
        Instant asOf) {
}
