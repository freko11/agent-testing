package com.autotrade.dashboard.marketdata;

/** Ticker is registered but the provider has no candle data for it (unknown to the provider, delisted, etc.). */
public class NoPriceDataException extends RuntimeException {

    public NoPriceDataException(String message) {
        super(message);
    }
}
