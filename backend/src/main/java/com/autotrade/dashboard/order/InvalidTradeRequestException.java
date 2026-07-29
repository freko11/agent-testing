package com.autotrade.dashboard.order;

/** A trade request failed server-side validation (leverage bounds, TP/SL on the wrong side of price, non-positive computed quantity). */
public class InvalidTradeRequestException extends RuntimeException {

    public InvalidTradeRequestException(String message) {
        super(message);
    }
}
