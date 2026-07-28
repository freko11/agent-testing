package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Deterministic candle fixtures + hand/script-computed reference values for indicator math,
 * reused across RSI/MACD/MA-crossover/volatility/volume-trend unit tests. Fully satisfies
 * E1-F4-S2 ("fixture dataset checked in; reused by indicator unit tests") — the OHLCV_40
 * dataset below (real high/low spread + varying volume) closes the one gap the RSI/MACD/MA
 * fixture set (degenerate high=low=close, constant volume) deliberately left open.
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

    // ATR-14/volume-trend reference values against CLOSES_40's degenerate (high=low=close, constant
    // 1,000,000 volume) candles — used by IndicatorServiceTest, which reuses candles40() for RSI/MACD/MA too.
    static final BigDecimal ATR_PCT_DEGENERATE_FULL = new BigDecimal("0.4842");
    static final BigDecimal VOLUME_TREND_DEGENERATE_FULL = new BigDecimal("1.0000");
    static final BigDecimal VOLUME_DEGENERATE_FULL = new BigDecimal("1000000.0000");

    /** High = close * 1.008, low = close * 0.992 (~1.6% daily range) — real high/low spread, unlike CLOSES_40. */
    static final BigDecimal[] HIGHS_40 = {
            new BigDecimal("100.80"), new BigDecimal("102.01"), new BigDecimal("102.82"), new BigDecimal("102.31"),
            new BigDecimal("103.42"), new BigDecimal("103.72"), new BigDecimal("103.52"), new BigDecimal("104.43"),
            new BigDecimal("105.94"), new BigDecimal("105.24"), new BigDecimal("105.64"), new BigDecimal("106.24"),
            new BigDecimal("105.94"), new BigDecimal("106.95"), new BigDecimal("107.15"), new BigDecimal("106.24"),
            new BigDecimal("106.75"), new BigDecimal("108.06"), new BigDecimal("107.65"), new BigDecimal("108.36"),
            new BigDecimal("108.46"), new BigDecimal("107.86"), new BigDecimal("108.66"), new BigDecimal("109.57"),
            new BigDecimal("109.37"), new BigDecimal("109.77"), new BigDecimal("110.88"), new BigDecimal("110.38"),
            new BigDecimal("110.98"), new BigDecimal("111.28"), new BigDecimal("111.18"), new BigDecimal("111.89"),
            new BigDecimal("112.39"), new BigDecimal("112.09"), new BigDecimal("113.00"), new BigDecimal("113.20"),
            new BigDecimal("112.80"), new BigDecimal("113.40"), new BigDecimal("114.21"), new BigDecimal("114.00"),
    };
    static final BigDecimal[] LOWS_40 = {
            new BigDecimal("99.20"), new BigDecimal("100.39"), new BigDecimal("101.18"), new BigDecimal("100.69"),
            new BigDecimal("101.78"), new BigDecimal("102.08"), new BigDecimal("101.88"), new BigDecimal("102.77"),
            new BigDecimal("104.26"), new BigDecimal("103.56"), new BigDecimal("103.96"), new BigDecimal("104.56"),
            new BigDecimal("104.26"), new BigDecimal("105.25"), new BigDecimal("105.45"), new BigDecimal("104.56"),
            new BigDecimal("105.05"), new BigDecimal("106.34"), new BigDecimal("105.95"), new BigDecimal("106.64"),
            new BigDecimal("106.74"), new BigDecimal("106.14"), new BigDecimal("106.94"), new BigDecimal("107.83"),
            new BigDecimal("107.63"), new BigDecimal("108.03"), new BigDecimal("109.12"), new BigDecimal("108.62"),
            new BigDecimal("109.22"), new BigDecimal("109.52"), new BigDecimal("109.42"), new BigDecimal("110.11"),
            new BigDecimal("110.61"), new BigDecimal("110.31"), new BigDecimal("111.20"), new BigDecimal("111.40"),
            new BigDecimal("111.00"), new BigDecimal("111.60"), new BigDecimal("112.39"), new BigDecimal("112.20"),
    };

    // Full/first-34/minimum(15)-candle ATR-14% reference values against HIGHS_40/LOWS_40.
    static final BigDecimal ATR_PCT_FULL = new BigDecimal("1.5793");
    static final BigDecimal ATR_PCT_34 = new BigDecimal("1.5974");
    static final BigDecimal ATR_PCT_15 = new BigDecimal("1.6671");

    /** Flat volume for 30 candles, then a clear step-up for the last 10 — an unambiguous "volume trending up" case. */
    static final BigDecimal[] VOLUMES_STEP_UP_40 = volumes(30, "1000000", 10, "3000000");
    /** Mirror of the step-up case: high volume tapering off — an unambiguous "volume drying up" case. */
    static final BigDecimal[] VOLUMES_DECLINING_40 = volumes(30, "3000000", 10, "1000000");
    /** All-zero volume: the long-period SMA is zero, exercising VolumeTrendCalculator's null-return path. */
    static final BigDecimal[] VOLUMES_ZERO_40 = volumes(40, "0", 0, "0");

    static final BigDecimal VOLUME_TREND_STEP_UP_FULL = new BigDecimal("1.8000");
    static final BigDecimal VOLUME_TREND_DECLINING_FULL = new BigDecimal("0.4286");

    private static BigDecimal[] volumes(int firstCount, String firstValue, int secondCount, String secondValue) {
        BigDecimal[] result = new BigDecimal[firstCount + secondCount];
        Arrays.fill(result, 0, firstCount, new BigDecimal(firstValue));
        Arrays.fill(result, firstCount, firstCount + secondCount, new BigDecimal(secondValue));
        return result;
    }

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

    private static List<Candle> candlesWithSpreadAndVolume(BigDecimal[] closes, BigDecimal[] highs, BigDecimal[] lows,
                                                             BigDecimal[] volumes) {
        List<Candle> result = new ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            result.add(new Candle(BASE.plus(i, ChronoUnit.DAYS), closes[i], highs[i], lows[i], closes[i], volumes[i]));
        }
        return result;
    }

    /** Real high/low spread + step-up volume (last 10 candles), full 40-candle series. */
    static List<Candle> ohlcv40StepUpVolume() {
        return candlesWithSpreadAndVolume(CLOSES_40, HIGHS_40, LOWS_40, VOLUMES_STEP_UP_40);
    }

    /** Same closes/highs/lows as {@link #ohlcv40StepUpVolume()}, first {@code n} candles only. */
    static List<Candle> ohlcv40First(int n) {
        return candlesWithSpreadAndVolume(Arrays.copyOf(CLOSES_40, n), Arrays.copyOf(HIGHS_40, n),
                Arrays.copyOf(LOWS_40, n), Arrays.copyOf(VOLUMES_STEP_UP_40, n));
    }

    /** Real high/low spread + declining volume (last 10 candles) — the "volume drying up" mirror case. */
    static List<Candle> ohlcv40DecliningVolume() {
        return candlesWithSpreadAndVolume(CLOSES_40, HIGHS_40, LOWS_40, VOLUMES_DECLINING_40);
    }

    /** Real high/low spread + all-zero volume — exercises VolumeTrendCalculator's null-return path. */
    static List<Candle> ohlcv40ZeroVolume() {
        return candlesWithSpreadAndVolume(CLOSES_40, HIGHS_40, LOWS_40, VOLUMES_ZERO_40);
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
