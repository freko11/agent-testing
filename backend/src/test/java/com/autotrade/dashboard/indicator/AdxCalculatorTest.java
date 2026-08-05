package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S2: pins {@link AdxCalculator}'s Wilder ADX math down exactly with hand-computed
 * synthetic candles at period=2 (small enough to verify by hand) — the two real
 * BTCUSDT/DOGEUSDT backtest fixtures can't provide ground truth for the algorithm itself, only
 * evidence under review (same rationale {@code BacktestHarnessTpSlTest} documents for the TP/SL
 * crossing scan).
 *
 * <p>The uptrend fixture is engineered so every move is a clean higher-high/higher-low (no down
 * moves at all), which makes -DM always zero and therefore -DI always zero and DX always exactly
 * 100 at every step, regardless of magnitude — an exact, rounding-free reference value. The chop
 * fixture alternates up/down moves; its exact ADX (26.6667) was hand-derived from the same
 * algorithm using exact fractions (100/3 and 20 for the two DX steps, averaged to 80/3), not
 * approximated, so this is a real pinned reference value, not a "close enough" tolerance check.
 */
class AdxCalculatorTest {

    @Test
    void cleanUptrend_periodTwo_adxIsExactlyOneHundred() {
        List<Candle> candles = candles(
                bar(102, 98, 100),
                bar(104, 100, 103),
                bar(107, 102, 105),
                bar(110, 104, 108));

        BigDecimal adx = AdxCalculator.calculate(candles, 2);

        assertEquals(new BigDecimal("100.0000"), adx,
                "every move is a clean higher-high/higher-low, so -DM is always zero and DX is always exactly 100");
    }

    @Test
    void choppyAlternatingMoves_periodTwo_matchesHandDerivedValue() {
        List<Candle> candles = candles(
                bar(102, 98, 100),
                bar(101, 97, 98),
                bar(103, 96, 101),
                bar(100, 95, 97));

        BigDecimal adx = AdxCalculator.calculate(candles, 2);

        assertEquals(new BigDecimal("26.6667"), adx,
                "hand-derived from exact fractions: DX steps of 100/3 and 20, averaged to 80/3 = 26.6667");
    }

    @Test
    void chopHasLowerAdxThanCleanTrend_sameCandleCount() {
        List<Candle> uptrend = candles(
                bar(102, 98, 100), bar(104, 100, 103), bar(107, 102, 105), bar(110, 104, 108));
        List<Candle> chop = candles(
                bar(102, 98, 100), bar(101, 97, 98), bar(103, 96, 101), bar(100, 95, 97));

        BigDecimal uptrendAdx = AdxCalculator.calculate(uptrend, 2);
        BigDecimal chopAdx = AdxCalculator.calculate(chop, 2);

        assertTrue(chopAdx.compareTo(uptrendAdx) < 0, "a choppy, direction-reversing series must score a lower ADX than a clean one-directional trend");
    }

    @Test
    void fewerThanTwicePeriodCandles_throws() {
        List<Candle> candles = candles(bar(102, 98, 100), bar(104, 100, 103), bar(107, 102, 105));

        assertThrows(IllegalArgumentException.class, () -> AdxCalculator.calculate(candles, 2));
    }

    private static Candle bar(double high, double low, double close) {
        return new Candle(Instant.EPOCH, BigDecimal.valueOf(close), BigDecimal.valueOf(high), BigDecimal.valueOf(low),
                BigDecimal.valueOf(close), BigDecimal.ONE);
    }

    private static List<Candle> candles(Candle... bars) {
        List<Candle> result = new ArrayList<>();
        Instant timestamp = Instant.EPOCH;
        for (Candle bar : bars) {
            result.add(new Candle(timestamp, bar.open(), bar.high(), bar.low(), bar.close(), bar.volume()));
            timestamp = timestamp.plus(1, ChronoUnit.DAYS);
        }
        return result;
    }
}
