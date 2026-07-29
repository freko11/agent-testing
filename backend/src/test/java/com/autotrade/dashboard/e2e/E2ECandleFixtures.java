package com.autotrade.dashboard.e2e;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A purpose-built 40-daily-candle series for {@link TickerSignalOrderE2ETest}, deliberately not
 * reused from {@code com.autotrade.dashboard.indicator.IndicatorTestFixtures} (package-private to
 * that package anyway, and its degenerate {@code CLOSES_40} series actually evaluates to
 * {@code CONFLICTING_SIGNALS} under {@code SignalRuleEngine} — RSI overbought/bearish, MACD/MA
 * bullish — not a usable directional call for this test). This fixture only needs to land
 * reliably in one rule-table bucket, not hit exact independently-verified reference values like
 * {@code IndicatorTestFixtures} does: the first 26 candles chop without net drift, the last 14
 * add a gentle uptrend so MACD's histogram and the SMA10/SMA30 crossover both read bullish, while
 * RSI-14 stays inside its neutral 30-70 band. A naive straight-line uptrend instead pushes RSI
 * toward overbought (bearish), which collides with MACD/MA's bullish read and produces
 * {@code CONFLICTING_SIGNALS} rather than a clean {@code BULLISH_MAJORITY} — confirmed with an
 * independent Python/Decimal script mirroring {@code RsiCalculator}/{@code MacdCalculator}/
 * {@code MovingAverageCrossoverCalculator}/{@code VolatilityCalculator}/{@code
 * VolumeTrendCalculator} exactly before these numbers were finalized, same discipline as
 * E2-F2-S1/S2's reference values.
 *
 * <p>Independently computed reference values for this exact series: RSI-14 = 57.7870,
 * MACD(12,26,9) histogram = +0.08595536 (bullish), SMA10 = 100.787 &gt; SMA30 = 100.278
 * ({@code SHORT_ABOVE_LONG}, bullish), ATR% = 1.5920 (LOW volatility band), volume-trend ratio =
 * 1.0000 (constant volume, comfortably above the 0.20 dried-up gate) — together these evaluate to
 * {@code SignalRuleId.BULLISH_MAJORITY} / {@code HoldTermRule.MODERATE_LOW} ("3-10 days").
 *
 * <p>{@code open == close} for every candle (degenerate, matching {@code IndicatorTestFixtures}'
 * own precedent) since no indicator here reads the open price; only {@code high}/{@code low}
 * carry a real ~1.6% daily spread, needed for a non-trivial ATR% reading.
 */
final class E2ECandleFixtures {

    private static final BigDecimal[] CLOSES = {
            new BigDecimal("100.00"), new BigDecimal("100.39"), new BigDecimal("100.49"), new BigDecimal("100.21"),
            new BigDecimal("99.78"), new BigDecimal("99.51"), new BigDecimal("99.61"), new BigDecimal("100.01"),
            new BigDecimal("100.40"), new BigDecimal("100.48"), new BigDecimal("100.21"), new BigDecimal("99.77"),
            new BigDecimal("99.51"), new BigDecimal("99.62"), new BigDecimal("100.02"), new BigDecimal("100.40"),
            new BigDecimal("100.48"), new BigDecimal("100.20"), new BigDecimal("99.76"), new BigDecimal("99.51"),
            new BigDecimal("99.62"), new BigDecimal("100.03"), new BigDecimal("100.41"), new BigDecimal("100.48"),
            new BigDecimal("100.19"), new BigDecimal("99.76"), new BigDecimal("99.73"), new BigDecimal("99.90"),
            new BigDecimal("100.26"), new BigDecimal("100.61"), new BigDecimal("100.73"), new BigDecimal("100.61"),
            new BigDecimal("100.38"), new BigDecimal("100.29"), new BigDecimal("100.47"), new BigDecimal("100.83"),
            new BigDecimal("101.17"), new BigDecimal("101.29"), new BigDecimal("101.16"), new BigDecimal("100.94"),
    };

    private static final BigDecimal[] HIGHS = {
            new BigDecimal("100.80"), new BigDecimal("101.19"), new BigDecimal("101.29"), new BigDecimal("101.01"),
            new BigDecimal("100.58"), new BigDecimal("100.31"), new BigDecimal("100.41"), new BigDecimal("100.81"),
            new BigDecimal("101.20"), new BigDecimal("101.28"), new BigDecimal("101.01"), new BigDecimal("100.57"),
            new BigDecimal("100.31"), new BigDecimal("100.42"), new BigDecimal("100.82"), new BigDecimal("101.20"),
            new BigDecimal("101.28"), new BigDecimal("101.00"), new BigDecimal("100.56"), new BigDecimal("100.31"),
            new BigDecimal("100.42"), new BigDecimal("100.83"), new BigDecimal("101.21"), new BigDecimal("101.28"),
            new BigDecimal("100.99"), new BigDecimal("100.56"), new BigDecimal("100.53"), new BigDecimal("100.70"),
            new BigDecimal("101.06"), new BigDecimal("101.41"), new BigDecimal("101.54"), new BigDecimal("101.41"),
            new BigDecimal("101.18"), new BigDecimal("101.09"), new BigDecimal("101.27"), new BigDecimal("101.64"),
            new BigDecimal("101.98"), new BigDecimal("102.10"), new BigDecimal("101.97"), new BigDecimal("101.75"),
    };

    private static final BigDecimal[] LOWS = {
            new BigDecimal("99.20"), new BigDecimal("99.59"), new BigDecimal("99.69"), new BigDecimal("99.41"),
            new BigDecimal("98.98"), new BigDecimal("98.71"), new BigDecimal("98.81"), new BigDecimal("99.21"),
            new BigDecimal("99.60"), new BigDecimal("99.68"), new BigDecimal("99.41"), new BigDecimal("98.97"),
            new BigDecimal("98.71"), new BigDecimal("98.82"), new BigDecimal("99.22"), new BigDecimal("99.60"),
            new BigDecimal("99.68"), new BigDecimal("99.40"), new BigDecimal("98.96"), new BigDecimal("98.71"),
            new BigDecimal("98.82"), new BigDecimal("99.23"), new BigDecimal("99.61"), new BigDecimal("99.68"),
            new BigDecimal("99.39"), new BigDecimal("98.96"), new BigDecimal("98.93"), new BigDecimal("99.10"),
            new BigDecimal("99.46"), new BigDecimal("99.81"), new BigDecimal("99.92"), new BigDecimal("99.81"),
            new BigDecimal("99.58"), new BigDecimal("99.49"), new BigDecimal("99.67"), new BigDecimal("100.02"),
            new BigDecimal("100.36"), new BigDecimal("100.48"), new BigDecimal("100.35"), new BigDecimal("100.13"),
    };

    private static final BigDecimal VOLUME = new BigDecimal("1000");

    private E2ECandleFixtures() {
    }

    static List<Candle> bullishCandles() {
        Instant start = Instant.parse("2024-01-01T00:00:00Z");
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < CLOSES.length; i++) {
            candles.add(new Candle(start.plus(i, ChronoUnit.DAYS), CLOSES[i], HIGHS[i], LOWS[i], CLOSES[i], VOLUME));
        }
        return candles;
    }
}
