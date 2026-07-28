package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic candle fixtures + hand/script-computed reference values for indicator math,
 * reused across RSI/MACD/MA-crossover unit tests. Also satisfies E1-F4-S2's "fixture dataset
 * checked in; reused by indicator unit tests" for this indicator set — only E2-F2-S2's
 * volatility/volume fixture (needing real high/low spread, unlike this degenerate OHLC data)
 * remains open.
 */
final class IndicatorTestFixtures {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    /** 40 daily closes. Reference values below computed with Decimal(prec=50) arithmetic, HALF_UP final rounding. */
    static final BigDecimal[] CLOSES_40 = {
            new BigDecimal("100.00"), new BigDecimal("101.20"), new BigDecimal("102.00"), new BigDecimal("101.50"),
            new BigDecimal("102.60"), new BigDecimal("102.90"), new BigDecimal("102.70"), new BigDecimal("103.60"),
            new BigDecimal("105.10"), new BigDecimal("104.40"), new BigDecimal("104.80"), new BigDecimal("105.40"),
            new BigDecimal("105.10"), new BigDecimal("106.10"), new BigDecimal("106.30"), new BigDecimal("105.40"),
            new BigDecimal("105.90"), new BigDecimal("107.20"), new BigDecimal("106.80"), new BigDecimal("107.50"),
            new BigDecimal("107.60"), new BigDecimal("107.00"), new BigDecimal("107.80"), new BigDecimal("108.70"),
            new BigDecimal("108.50"), new BigDecimal("108.90"), new BigDecimal("110.00"), new BigDecimal("109.50"),
            new BigDecimal("110.10"), new BigDecimal("110.40"), new BigDecimal("110.30"), new BigDecimal("111.00"),
            new BigDecimal("111.50"), new BigDecimal("111.20"), new BigDecimal("112.10"), new BigDecimal("112.30"),
            new BigDecimal("111.90"), new BigDecimal("112.50"), new BigDecimal("113.30"), new BigDecimal("113.10"),
    };

    // Full 40-candle series reference values.
    static final BigDecimal RSI_14_FULL = new BigDecimal("77.8751");
    static final BigDecimal MACD_LINE_FULL = new BigDecimal("2.11694333");
    static final BigDecimal MACD_SIGNAL_FULL = new BigDecimal("2.13097767");
    static final BigDecimal MACD_HISTOGRAM_FULL = new BigDecimal("-0.01403434");
    static final BigDecimal SMA_10_FULL = new BigDecimal("111.92000000");
    static final BigDecimal SMA_30_FULL = new BigDecimal("108.94000000");

    // First-34-candles (minimum valid count) reference values — exercises the exact MACD signal seed point.
    static final BigDecimal MACD_LINE_34 = new BigDecimal("2.13398891");
    static final BigDecimal MACD_SIGNAL_34 = new BigDecimal("2.13697484");
    static final BigDecimal MACD_HISTOGRAM_34 = new BigDecimal("-0.00298593");

    private IndicatorTestFixtures() {
    }

    static List<Candle> candles(BigDecimal[] closes) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            BigDecimal close = closes[i];
            result.add(new Candle(BASE.plus(i, ChronoUnit.DAYS), close, close, close, close,
                    BigDecimal.valueOf(1_000_000)));
        }
        return result;
    }

    static List<Candle> candles40() {
        return candles(CLOSES_40);
    }

    static List<Candle> candlesFirst(int n) {
        BigDecimal[] slice = new BigDecimal[n];
        System.arraycopy(CLOSES_40, 0, slice, 0, n);
        return candles(slice);
    }

    /** Strictly increasing closes: every delta is a gain, so RSI must be exactly 100. */
    static List<Candle> allGainsCandles(int n) {
        BigDecimal[] closes = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            closes[i] = new BigDecimal("100.00").add(BigDecimal.valueOf(i));
        }
        return candles(closes);
    }

    /** Identical closes throughout: no movement at all, so RSI must be the neutral 50, not 100. */
    static List<Candle> flatCandles(int n) {
        BigDecimal[] closes = new BigDecimal[n];
        for (int i = 0; i < n; i++) {
            closes[i] = new BigDecimal("100.00");
        }
        return candles(closes);
    }

    /** 40 closes strictly decreasing by 1.00/day: SMA10 < SMA30 by arithmetic-sequence averaging, always. */
    static List<Candle> bearishCrossoverCandles() {
        BigDecimal[] closes = new BigDecimal[40];
        for (int i = 0; i < 40; i++) {
            closes[i] = new BigDecimal("140.00").subtract(BigDecimal.valueOf(i));
        }
        return candles(closes);
    }
}
