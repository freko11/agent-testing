package com.autotrade.dashboard.common;

/**
 * Paper vs. live trading mode. Shared by {@code Order.orderMode} (which mode
 * an order was placed under) and {@code BrokerCredential.environment} (which
 * credential set to use for that mode).
 */
public enum TradingMode {
    PAPER,
    LIVE
}
