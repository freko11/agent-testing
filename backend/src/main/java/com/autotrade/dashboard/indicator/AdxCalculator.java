package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Wilder's Average Directional Index (ADX) — a trend-strength/regime indicator (E8-F3-S2),
 * independent of {@link VolatilityCalculator}'s ATR% (which measures volatility magnitude, not
 * directional persistence): a choppy, whipsawing market can be volatile without trending, and a
 * quiet, grinding market can trend without much volatility. High ADX means the recent directional
 * move has been persistent (a trend); low ADX means price has been chopping without net direction
 * (ranging). Feeds {@code com.autotrade.dashboard.signal.RegimeClassifier}.
 *
 * <p>Deliberately duplicates its own true-range computation rather than sharing {@link
 * VolatilityCalculator}'s (private) helper — every calculator in this package is independently
 * pure with no cross-calculator calls, and this preserves that convention rather than being the
 * first to break it. Candles must be ascending by timestamp (oldest first), the same contract
 * every {@link com.autotrade.dashboard.marketdata.MarketDataClient} guarantees.
 *
 * <p>Wilder's smoothing here uses the running-average form (matching {@link VolatilityCalculator}'s
 * own ATR recursion: {@code smoothed = (smoothed * (period - 1) + current) / period}), not the
 * running-sum form some references use — algebraically equivalent for +DI/-DI's ratio, since both
 * numerator and denominator scale identically either way.
 */
public final class AdxCalculator {

    public static final int DEFAULT_PERIOD = 14;
    private static final MathContext MC = new MathContext(50);

    private AdxCalculator() {
    }

    public static BigDecimal calculate(List<Candle> candles, int period) {
        int minCandles = 2 * period;
        if (candles.size() < minCandles) {
            throw new IllegalArgumentException(
                    "ADX-" + period + " requires at least " + minCandles + " candles, got " + candles.size());
        }

        int n = candles.size();
        BigDecimal[] plusDm = new BigDecimal[n];
        BigDecimal[] minusDm = new BigDecimal[n];
        BigDecimal[] tr = new BigDecimal[n];
        for (int i = 1; i < n; i++) {
            Candle curr = candles.get(i);
            Candle prev = candles.get(i - 1);
            BigDecimal upMove = curr.high().subtract(prev.high());
            BigDecimal downMove = prev.low().subtract(curr.low());
            plusDm[i] = (upMove.signum() > 0 && upMove.compareTo(downMove) > 0) ? upMove : BigDecimal.ZERO;
            minusDm[i] = (downMove.signum() > 0 && downMove.compareTo(upMove) > 0) ? downMove : BigDecimal.ZERO;
            BigDecimal highLow = curr.high().subtract(curr.low());
            BigDecimal highPrevClose = curr.high().subtract(prev.close()).abs();
            BigDecimal lowPrevClose = curr.low().subtract(prev.close()).abs();
            tr[i] = highLow.max(highPrevClose).max(lowPrevClose);
        }

        BigDecimal smoothedPlusDm = average(plusDm, 1, period);
        BigDecimal smoothedMinusDm = average(minusDm, 1, period);
        BigDecimal smoothedTr = average(tr, 1, period);

        List<BigDecimal> dxValues = new ArrayList<>();
        dxValues.add(dx(smoothedPlusDm, smoothedMinusDm, smoothedTr));

        for (int i = period + 1; i < n; i++) {
            smoothedPlusDm = wilderSmooth(smoothedPlusDm, plusDm[i], period);
            smoothedMinusDm = wilderSmooth(smoothedMinusDm, minusDm[i], period);
            smoothedTr = wilderSmooth(smoothedTr, tr[i], period);
            dxValues.add(dx(smoothedPlusDm, smoothedMinusDm, smoothedTr));
        }

        BigDecimal adx = average(dxValues.subList(0, period));
        for (int i = period; i < dxValues.size(); i++) {
            adx = wilderSmooth(adx, dxValues.get(i), period);
        }
        return adx.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal dx(BigDecimal smoothedPlusDm, BigDecimal smoothedMinusDm, BigDecimal smoothedTr) {
        if (smoothedTr.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal plusDi = smoothedPlusDm.multiply(BigDecimal.valueOf(100)).divide(smoothedTr, MC);
        BigDecimal minusDi = smoothedMinusDm.multiply(BigDecimal.valueOf(100)).divide(smoothedTr, MC);
        BigDecimal diSum = plusDi.add(minusDi);
        if (diSum.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return plusDi.subtract(minusDi).abs().multiply(BigDecimal.valueOf(100)).divide(diSum, MC);
    }

    private static BigDecimal wilderSmooth(BigDecimal prevAvg, BigDecimal current, int period) {
        return prevAvg.multiply(BigDecimal.valueOf(period - 1)).add(current).divide(BigDecimal.valueOf(period), MC);
    }

    private static BigDecimal average(BigDecimal[] values, int fromInclusive, int count) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = fromInclusive; i < fromInclusive + count; i++) {
            sum = sum.add(values[i]);
        }
        return sum.divide(BigDecimal.valueOf(count), MC);
    }

    private static BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), MC);
    }
}
