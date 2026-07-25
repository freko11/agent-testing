package com.autotrade.dashboard.broker;

/**
 * A supported broker/exchange. Shared by {@code BrokerCredential.broker},
 * {@code Order.broker} (denormalized snapshot, independent of credential
 * rotation), and {@code IndicatorSnapshot.marketDataSource} (which upstream
 * feed the indicator inputs came from).
 */
public enum Broker {
    ALPACA,
    BINANCE
}
