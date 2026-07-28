package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.TickerSummary;
import com.autotrade.dashboard.ticker.Ticker;

import java.math.BigDecimal;
import java.time.Instant;

public record IndicatorResponse(TickerSummary ticker, Broker source, Instant asOf, BigDecimal price, BigDecimal rsi,
                                 MacdResult macd, MovingAverageResult movingAverage, BigDecimal volatility,
                                 BigDecimal volume, BigDecimal volumeTrend) {

    static IndicatorResponse from(Ticker ticker, Broker source, Candle latest, IndicatorService.BigDecimalIndicators indicators) {
        return new IndicatorResponse(TickerSummary.from(ticker), source, latest.timestamp(), latest.close(),
                indicators.rsi(), indicators.macd(), indicators.movingAverage(), indicators.volatility(),
                indicators.volume(), indicators.volumeTrend());
    }
}
