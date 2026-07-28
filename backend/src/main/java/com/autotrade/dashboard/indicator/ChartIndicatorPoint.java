package com.autotrade.dashboard.indicator;

import java.math.BigDecimal;
import java.time.Instant;

/** One walk-forward RSI/MA point for the E3-F2-S1 price chart, aligned by index to the candle at the same position. */
public record ChartIndicatorPoint(Instant timestamp, BigDecimal rsi, BigDecimal maShort, BigDecimal maLong) {
}
