package com.autotrade.dashboard.ticker;

/** Thrown when re-registering an existing symbol under a different asset type than it already has. */
public class TickerAssetTypeConflictException extends RuntimeException {

    public TickerAssetTypeConflictException(String symbol, AssetType existing, AssetType requested) {
        super("Ticker '" + symbol + "' is already registered as " + existing + "; cannot re-register as " + requested);
    }
}
