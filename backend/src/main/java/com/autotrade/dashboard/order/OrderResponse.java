package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.ticker.AssetType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single order for the status/history view (E5-F3-S1) — deliberately a
 * separate DTO from {@link TradeOrderResponse}, which is scoped to "the
 * result of a click-Trade submission" and is missing fields (ticker symbol,
 * TP/SL, leverage) a status page needs. Used for both the list view and the
 * single-order refresh response — no separate lightweight list DTO, per this
 * story's own "same shape is probably fine" scope.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderResponse(
        Long id,
        String tickerSymbol,
        AssetType assetType,
        Broker broker,
        TradingMode orderMode,
        OrderSide side,
        BigDecimal quantity,
        BigDecimal requestedAmountUsd,
        BigDecimal leverage,
        EntryOrderType entryOrderType,
        BigDecimal takeProfitPrice,
        BigDecimal stopLossPrice,
        BigDecimal entryPrice,
        String clientOrderId,
        String brokerOrderId,
        OrderStatus status,
        String rejectionReason,
        Instant submittedAt,
        Instant filledAt,
        Instant createdAt,
        Instant updatedAt) {

    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getTicker().getSymbol(), order.getAssetType(),
                order.getBroker(), order.getOrderMode(), order.getSide(), order.getQuantity(),
                order.getRequestedAmountUsd(), order.getLeverage(), order.getEntryOrderType(),
                order.getTakeProfitPrice(), order.getStopLossPrice(), order.getEntryPrice(),
                order.getClientOrderId(), order.getBrokerOrderId(), order.getStatus(), order.getRejectionReason(),
                order.getSubmittedAt(), order.getFilledAt(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
