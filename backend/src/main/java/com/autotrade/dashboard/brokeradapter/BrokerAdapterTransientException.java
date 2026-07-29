package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;

/**
 * A {@link BrokerAdapterException} caused by an ambiguous transport failure
 * (network unreachable, timeout, 5xx) that {@link RetryingBrokerAdapter}
 * treats as safe to retry on non-mutating calls — but never on {@code
 * placeOrder}, since the broker may already have received the order before
 * the connection dropped. Concrete adapters (F4.2/F4.3) throw this instead
 * of the plain base type when a failure is actually transient.
 */
public class BrokerAdapterTransientException extends BrokerAdapterException {

    public BrokerAdapterTransientException(Broker source, String message) {
        super(source, message);
    }

    public BrokerAdapterTransientException(Broker source, String message, Throwable cause) {
        super(source, message, cause);
    }
}
