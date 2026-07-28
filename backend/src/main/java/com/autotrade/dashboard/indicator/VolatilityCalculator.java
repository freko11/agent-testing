package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Wilder's Average True Range, normalized as a percentage of the latest close
 * ({@code ATR / close * 100}) so volatility is comparable across tickers of very
 * different price scales (a stock vs. a crypto pair), matching RSI's own
 * bounded-percentage precedent. Candles must be ascending by timestamp (oldest
 * first) — every {@link com.autotrade.dashboard.marketdata.MarketDataClient}
 * implementation guarantees this.
 */
public final class VolatilityCalculator {

    public static final int DEFAULT_PERIOD = 14;
    private static final MathContext MC = new MathContext(50);

    private VolatilityCalculator() {
    }

    public static BigDecimal calculate(List<Candle> candles, int period) {
        int minCandles = period + 1;
        if (candles.size() < minCandles) {
            throw new IllegalArgumentException(
                    "ATR-" + period + " requires at least " + minCandles + " candles, got " + candles.size());
        }

        BigDecimal atr = BigDecimal.ZERO;
        for (int i = 1; i <= period; i++) {
            atr = atr.add(trueRange(candles, i));
        }
        atr = atr.divide(BigDecimal.valueOf(period), MC);

        for (int i = period + 1; i < candles.size(); i++) {
            BigDecimal tr = trueRange(candles, i);
            atr = atr.multiply(BigDecimal.valueOf(period - 1)).add(tr).divide(BigDecimal.valueOf(period), MC);
        }

        BigDecimal lastClose = candles.get(candles.size() - 1).close();
        BigDecimal pct = atr.divide(lastClose, MC).multiply(BigDecimal.valueOf(100));
        return pct.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal trueRange(List<Candle> candles, int i) {
        Candle candle = candles.get(i);
        BigDecimal prevClose = candles.get(i - 1).close();
        BigDecimal highLow = candle.high().subtract(candle.low());
        BigDecimal highPrevClose = candle.high().subtract(prevClose).abs();
        BigDecimal lowPrevClose = candle.low().subtract(prevClose).abs();
        return highLow.max(highPrevClose).max(lowPrevClose);
    }
}
