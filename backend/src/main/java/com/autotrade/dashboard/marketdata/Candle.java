package com.autotrade.dashboard.marketdata;

import java.math.BigDecimal;
import java.time.Instant;

/** Provider-agnostic OHLCV bar. E2-F2's indicator calculation is the intended next consumer of this type. */
public record Candle(Instant timestamp, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                      BigDecimal volume) {
}
