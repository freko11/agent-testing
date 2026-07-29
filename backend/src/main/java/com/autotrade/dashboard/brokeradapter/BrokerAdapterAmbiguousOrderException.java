package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;

/**
 * Thrown only from {@link RetryingBrokerAdapter#placeOrder}, only when both
 * the original {@code placeOrder} attempt and the reconciliation {@code
 * getOrderStatus} probe fail — genuine ambiguity about whether the order
 * reached the broker. Distinct from {@link BrokerAdapterUnavailableException}
 * (which means reconciliation positively confirmed the order was never
 * submitted): a future controller/UI must render this as a stronger,
 * differently-worded state than plain unavailability, since blindly
 * resubmitting under a new identity here could create a real duplicate
 * order. The cause is the original {@code placeOrder} failure; the
 * reconciliation failure is attached via {@link #addSuppressed}.
 */
public class BrokerAdapterAmbiguousOrderException extends BrokerAdapterException {

    private final String clientOrderId;

    public BrokerAdapterAmbiguousOrderException(Broker source, String clientOrderId, Throwable cause) {
        super(source, "Unable to confirm whether order " + clientOrderId + " reached " + source
                + ". Do not submit a new order for this intent — retry with the SAME clientOrderId"
                + " (idempotent) once the broker is reachable, or verify manually via the broker's own"
                + " account view.", cause);
        this.clientOrderId = clientOrderId;
    }

    public String clientOrderId() {
        return clientOrderId;
    }
}
