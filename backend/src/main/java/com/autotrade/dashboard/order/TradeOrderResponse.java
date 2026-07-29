package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The result of a "click Trade" submission. Always HTTP 201 — a business
 * rejection, a partially-protected fill, and an infrastructure failure are
 * all normal values of {@code status}/{@code rejectionReason}, never a
 * second HTTP exception, so the frontend renders one response shape instead
 * of branching between a success type and a failure type.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TradeOrderResponse(
        Long orderId,
        String clientOrderId,
        String brokerOrderId,
        Broker broker,
        OrderSide side,
        BigDecimal quantity,
        OrderStatus status,
        BigDecimal filledPrice,
        String rejectionReason,
        Instant submittedAt) {

    static TradeOrderResponse from(Order order) {
        return new TradeOrderResponse(order.getId(), order.getClientOrderId(), order.getBrokerOrderId(),
                order.getBroker(), order.getSide(), order.getQuantity(), order.getStatus(), order.getEntryPrice(),
                order.getRejectionReason(), order.getSubmittedAt());
    }
}
