package com.autotrade.dashboard.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * The "click Trade" payload (E5-F2-S1) — amount/leverage/TP/SL only.
 * Direction and price are deliberately NOT part of this request: {@link
 * OrderService} always re-derives them by recomputing the signal
 * server-side, since a client-cached {@code SignalResponse} could be stale
 * by the time the user clicks Trade and this places a real bracket order.
 */
public record PlaceOrderRequest(
        @NotNull @Positive BigDecimal amountUsd,
        @NotNull @Positive BigDecimal leverage,
        @NotNull @Positive BigDecimal takeProfitPrice,
        @NotNull @Positive BigDecimal stopLossPrice) {
}
