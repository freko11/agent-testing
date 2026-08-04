package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.TickerSummary;

import java.util.List;

/**
 * Candles plus an aligned RSI/MA series for the E3-F2-S1 price chart. Unlike {@code /indicators} and {@code /signal},
 * this never throws {@link InsufficientPriceHistoryException} — {@code indicators} is simply empty below
 * {@link IndicatorService#MIN_CANDLES_FOR_INDICATORS} candles, since a candles-only chart is still a meaningful render.
 *
 * <p>{@code stale} is true when the market is closed and this is the last successfully fetched response for the
 * ticker, served from {@link IndicatorService}'s in-memory cache instead of a fresh (blocked) fetch — see
 * {@link IndicatorService#getChartData}.
 */
public record ChartDataResponse(TickerSummary ticker, Broker source, List<Candle> candles,
                                 List<ChartIndicatorPoint> indicators, boolean stale) {
}
