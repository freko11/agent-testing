package com.autotrade.dashboard.marketdata;

/** Ticker is a registered stock, but regular market hours haven't started yet or have already ended. */
public class MarketClosedException extends RuntimeException {

    public MarketClosedException(String symbol) {
        super("The stock market is closed. Regular hours are 9:30am-4:00pm America/New_York, Mon-Fri. "
                + "Price history for '" + symbol + "' isn't available as current data outside those hours.");
    }
}
