package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Wilder's RSI. Candles must be ascending by timestamp (oldest first) —
 * every {@link com.autotrade.dashboard.marketdata.MarketDataClient} implementation guarantees this.
 */
public final class RsiCalculator {

    public static final int DEFAULT_PERIOD = 14;
    private static final MathContext MC = new MathContext(50);

    private RsiCalculator() {
    }

    public static BigDecimal calculate(List<Candle> candles, int period) {
        int minCandles = period + 1;
        if (candles.size() < minCandles) {
            throw new IllegalArgumentException(
                    "RSI-" + period + " requires at least " + minCandles + " candles, got " + candles.size());
        }

        BigDecimal avgGain = BigDecimal.ZERO;
        BigDecimal avgLoss = BigDecimal.ZERO;
        for (int i = 1; i <= period; i++) {
            BigDecimal delta = candles.get(i).close().subtract(candles.get(i - 1).close());
            avgGain = avgGain.add(delta.max(BigDecimal.ZERO));
            avgLoss = avgLoss.add(delta.negate().max(BigDecimal.ZERO));
        }
        avgGain = avgGain.divide(BigDecimal.valueOf(period), MC);
        avgLoss = avgLoss.divide(BigDecimal.valueOf(period), MC);

        for (int i = period + 1; i < candles.size(); i++) {
            BigDecimal delta = candles.get(i).close().subtract(candles.get(i - 1).close());
            BigDecimal gain = delta.max(BigDecimal.ZERO);
            BigDecimal loss = delta.negate().max(BigDecimal.ZERO);
            avgGain = avgGain.multiply(BigDecimal.valueOf(period - 1)).add(gain).divide(BigDecimal.valueOf(period), MC);
            avgLoss = avgLoss.multiply(BigDecimal.valueOf(period - 1)).add(loss).divide(BigDecimal.valueOf(period), MC);
        }

        if (avgGain.signum() == 0 && avgLoss.signum() == 0) {
            return new BigDecimal("50.0000");
        }
        if (avgLoss.signum() == 0) {
            return new BigDecimal("100.0000");
        }
        BigDecimal rs = avgGain.divide(avgLoss, MC);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), MC));
        return rsi.setScale(4, RoundingMode.HALF_UP);
    }
}
