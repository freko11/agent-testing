package com.autotrade.dashboard.ticker;

/** Thrown when market data (or any per-ticker operation) is requested for a symbol never registered via POST /api/tickers. */
public class TickerNotRegisteredException extends RuntimeException {

    public TickerNotRegisteredException(String symbol) {
        super("Ticker '" + symbol + "' is not registered. Register it first via POST /api/tickers.");
    }
}
