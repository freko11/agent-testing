package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;

/** No {@code BrokerCredential} row exists for the broker/mode a trade needs to route through — an ops/config gap, not something the user can fix by retrying. */
public class BrokerCredentialNotConfiguredException extends RuntimeException {

    public BrokerCredentialNotConfiguredException(Broker broker, TradingMode mode) {
        super("No " + broker + " credential is configured for " + mode + " mode. Check the "
                + broker + " trading API key/secret environment variables.");
    }
}
