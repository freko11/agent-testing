package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;

import java.util.List;

public record PriceHistoryResponse(TickerSummary ticker, Broker source, List<Candle> candles) {

    static PriceHistoryResponse from(PriceHistoryResult result) {
        return new PriceHistoryResponse(TickerSummary.from(result.ticker()), result.source(), result.candles());
    }
}
