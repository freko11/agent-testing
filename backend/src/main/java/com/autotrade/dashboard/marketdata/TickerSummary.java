package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;

public record TickerSummary(Long id, String symbol, AssetType assetType, String exchange) {

    static TickerSummary from(Ticker ticker) {
        return new TickerSummary(ticker.getId(), ticker.getSymbol(), ticker.getAssetType(), ticker.getExchange());
    }
}
