package com.autotrade.dashboard.order;

/** Thrown when a trade is requested for a ticker whose freshly-recomputed signal is HOLD — no direction to size an entry for. */
public class SignalNotActionableException extends RuntimeException {

    public SignalNotActionableException(String symbol, String matchedRule) {
        super("\"" + symbol + "\" has no actionable BUY/SELL signal right now (current call: HOLD, rule: "
                + matchedRule + "). Nothing to trade.");
    }
}
