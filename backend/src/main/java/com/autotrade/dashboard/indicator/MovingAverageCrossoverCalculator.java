package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/**
 * Simple moving average crossover. Reports only the current short-vs-long relation,
 * not a "just crossed" event — event detection (diffing consecutive snapshots) belongs
 * to E2-F3's rule engine or E2-F4's backtester, not here.
 * <p>
 * Candles must be ascending by timestamp (oldest first) — every
 * {@link com.autotrade.dashboard.marketdata.MarketDataClient} implementation guarantees this.
 */
public final class MovingAverageCrossoverCalculator {

    public static final int DEFAULT_SHORT_PERIOD = 10;
    public static final int DEFAULT_LONG_PERIOD = 30;
    private static final MathContext MC = new MathContext(50);

    private MovingAverageCrossoverCalculator() {
    }

    public static MovingAverageResult calculate(List<Candle> candles, int shortPeriod, int longPeriod) {
        if (candles.size() < longPeriod) {
            throw new IllegalArgumentException(
                    "MA crossover(" + shortPeriod + "," + longPeriod + ") requires at least " + longPeriod
                            + " candles, got " + candles.size());
        }

        BigDecimal shortMa = sma(candles, shortPeriod, Candle::close).setScale(8, RoundingMode.HALF_UP);
        BigDecimal longMa = sma(candles, longPeriod, Candle::close).setScale(8, RoundingMode.HALF_UP);

        MovingAverageRelation relation;
        int cmp = shortMa.compareTo(longMa);
        if (cmp > 0) {
            relation = MovingAverageRelation.SHORT_ABOVE_LONG;
        } else if (cmp < 0) {
            relation = MovingAverageRelation.SHORT_BELOW_LONG;
        } else {
            relation = MovingAverageRelation.EQUAL;
        }

        BigDecimal lastClose = candles.get(candles.size() - 1).close();
        BigDecimal separationPctOfPrice = shortMa.subtract(longMa).abs()
                .divide(lastClose, MC).multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_UP);

        return new MovingAverageResult(shortPeriod, shortMa, longPeriod, longMa, relation, separationPctOfPrice);
    }

    /** Simple moving average of {@code valueOf(candle)} over the trailing {@code period} candles. */
    static BigDecimal sma(List<Candle> candles, int period, Function<Candle, BigDecimal> valueOf) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = candles.size() - period; i < candles.size(); i++) {
            sum = sum.add(valueOf.apply(candles.get(i)));
        }
        return sum.divide(BigDecimal.valueOf(period), MC);
    }
}
