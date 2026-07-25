package com.autotrade.dashboard.ticker;

/**
 * Which market a ticker belongs to. Drives which broker adapter and market
 * data source handle it (Alpaca for STOCK, Binance for CRYPTO).
 */
public enum AssetType {
    STOCK,
    CRYPTO
}
