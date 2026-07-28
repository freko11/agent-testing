package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.TickerSummary;

import java.util.List;

/**
 * Candles plus an aligned RSI/MA series for the E3-F2-S1 price chart. Unlike {@code /indicators} and {@code /signal},
 * this never throws {@link InsufficientPriceHistoryException} — {@code indicators} is simply empty below
 * {@link IndicatorService#MIN_CANDLES_FOR_INDICATORS} candles, since a candles-only chart is still a meaningful render.
 */
public record ChartDataResponse(TickerSummary ticker, Broker source, List<Candle> candles,
                                 List<ChartIndicatorPoint> indicators) {
}
