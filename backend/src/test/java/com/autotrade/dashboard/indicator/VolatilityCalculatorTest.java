package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VolatilityCalculatorTest {

    @Test
    void fullSeries_matchesReferenceValue() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40StepUpVolume();

        assertEquals(IndicatorTestFixtures.ATR_PCT_FULL,
                VolatilityCalculator.calculate(candles, VolatilityCalculator.DEFAULT_PERIOD));
    }

    @Test
    void first34Candles_matchesReferenceValue() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40First(34);

        assertEquals(IndicatorTestFixtures.ATR_PCT_34,
                VolatilityCalculator.calculate(candles, VolatilityCalculator.DEFAULT_PERIOD));
    }

    @Test
    void minimumCandles_periodPlusOne_matchesReferenceValue() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40First(15);

        assertEquals(IndicatorTestFixtures.ATR_PCT_15,
                VolatilityCalculator.calculate(candles, VolatilityCalculator.DEFAULT_PERIOD));
    }

    @Test
    void degenerateHighLowEqualsClose_reducesToCloseToCloseTrueRange() {
        List<Candle> candles = IndicatorTestFixtures.candles40();

        assertEquals(IndicatorTestFixtures.ATR_PCT_DEGENERATE_FULL,
                VolatilityCalculator.calculate(candles, VolatilityCalculator.DEFAULT_PERIOD));
    }

    @Test
    void fewerThanPeriodPlusOneCandles_throws() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40First(14);

        assertThrows(IllegalArgumentException.class,
                () -> VolatilityCalculator.calculate(candles, VolatilityCalculator.DEFAULT_PERIOD));
    }
}
