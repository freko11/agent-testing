package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.ticker.Ticker;

import java.util.List;

/** Internal result type — also the natural handoff point for E2-F2's indicator calculation to reuse without a second ticker lookup. */
public record PriceHistoryResult(Ticker ticker, Broker source, List<Candle> candles) {
}
