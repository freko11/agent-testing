package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Volume trend expressed as the ratio of a short-period volume SMA to a
 * long-period volume SMA (reusing {@link MovingAverageCrossoverCalculator}'s
 * SMA helper against {@link Candle#volume()} instead of close) — a ratio above
 * 1 means recent volume is trending up versus the longer baseline, below 1 means
 * it's drying up. Candles must be ascending by timestamp (oldest first) — every
 * {@link com.autotrade.dashboard.marketdata.MarketDataClient} implementation
 * guarantees this.
 */
public final class VolumeTrendCalculator {

    public static final int DEFAULT_SHORT_PERIOD = 10;
    public static final int DEFAULT_LONG_PERIOD = 30;

    private VolumeTrendCalculator() {
    }

    /** Returns {@code null} when the long-period volume SMA is zero (no volume at all over that window) — a
     * legitimate "dead ticker" reading, not a caller error, so no exception is thrown for it. */
    public static BigDecimal calculate(List<Candle> candles, int shortPeriod, int longPeriod) {
        if (candles.size() < longPeriod) {
            throw new IllegalArgumentException(
                    "Volume trend(" + shortPeriod + "," + longPeriod + ") requires at least " + longPeriod
                            + " candles, got " + candles.size());
        }

        BigDecimal shortSma = MovingAverageCrossoverCalculator.sma(candles, shortPeriod, Candle::volume);
        BigDecimal longSma = MovingAverageCrossoverCalculator.sma(candles, longPeriod, Candle::volume);

        if (longSma.signum() == 0) {
            return null;
        }

        return shortSma.divide(longSma, 4, RoundingMode.HALF_UP);
    }
}
