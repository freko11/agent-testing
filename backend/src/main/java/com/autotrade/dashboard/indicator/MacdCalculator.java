package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * MACD via the standard SMA-seeded EMA convention. Candles must be ascending
 * by timestamp (oldest first) — every {@link com.autotrade.dashboard.marketdata.MarketDataClient}
 * implementation guarantees this.
 */
public final class MacdCalculator {

    public static final int DEFAULT_FAST_PERIOD = 12;
    public static final int DEFAULT_SLOW_PERIOD = 26;
    public static final int DEFAULT_SIGNAL_PERIOD = 9;
    private static final MathContext MC = new MathContext(50);

    private MacdCalculator() {
    }

    public static MacdResult calculate(List<Candle> candles, int fastPeriod, int slowPeriod, int signalPeriod) {
        int minCandles = slowPeriod + signalPeriod - 1;
        if (candles.size() < minCandles) {
            throw new IllegalArgumentException(
                    "MACD(" + fastPeriod + "," + slowPeriod + "," + signalPeriod + ") requires at least "
                            + minCandles + " candles, got " + candles.size());
        }

        List<BigDecimal> closes = candles.stream().map(Candle::close).toList();
        BigDecimal[] emaFast = ema(closes, fastPeriod);
        BigDecimal[] emaSlow = ema(closes, slowPeriod);

        List<BigDecimal> macdLine = new ArrayList<>();
        for (int i = slowPeriod - 1; i < closes.size(); i++) {
            macdLine.add(emaFast[i].subtract(emaSlow[i]));
        }

        BigDecimal[] signalEma = ema(macdLine, signalPeriod);

        BigDecimal line = macdLine.get(macdLine.size() - 1);
        BigDecimal signal = signalEma[signalEma.length - 1];
        BigDecimal histogram = line.subtract(signal);
        BigDecimal lastClose = closes.get(closes.size() - 1);
        BigDecimal histogramPctOfPrice = histogram.abs().divide(lastClose, MC).multiply(BigDecimal.valueOf(100));

        return new MacdResult(
                line.setScale(8, RoundingMode.HALF_UP),
                signal.setScale(8, RoundingMode.HALF_UP),
                histogram.setScale(8, RoundingMode.HALF_UP),
                histogramPctOfPrice.setScale(4, RoundingMode.HALF_UP));
    }

    /** Standard EMA: seeded by the SMA of the first {@code period} values at index {@code period - 1}. */
    private static BigDecimal[] ema(List<BigDecimal> values, int period) {
        BigDecimal[] result = new BigDecimal[values.size()];
        BigDecimal k = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(period + 1), MC);
        BigDecimal seedSum = BigDecimal.ZERO;
        for (int i = 0; i < period; i++) {
            seedSum = seedSum.add(values.get(i));
        }
        result[period - 1] = seedSum.divide(BigDecimal.valueOf(period), MC);
        for (int i = period; i < values.size(); i++) {
            result[i] = values.get(i).multiply(k, MC)
                    .add(result[i - 1].multiply(BigDecimal.ONE.subtract(k), MC));
        }
        return result;
    }
}
