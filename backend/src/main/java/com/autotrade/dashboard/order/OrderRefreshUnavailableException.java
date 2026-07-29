package com.autotrade.dashboard.order;

/**
 * A refresh's call to {@code BrokerAdapter.getOrderStatus} threw
 * ({@code BrokerAdapterException} or a subtype) rather than answering —
 * broker down, rate-limited, or a fatal transport fault. The stored order
 * row is deliberately left untouched when this is thrown (see
 * {@code OrderService.refreshOrder}) rather than overwritten with a guess.
 */
public class OrderRefreshUnavailableException extends RuntimeException {

    public OrderRefreshUnavailableException(String message) {
        super(message);
    }
}
