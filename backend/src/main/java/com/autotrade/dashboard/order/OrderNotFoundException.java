package com.autotrade.dashboard.order;

/** No {@code Order} row exists for the requested id — a bad/stale id, not a transient failure. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("No order found with id " + orderId + ".");
    }
}
