package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VolumeTrendCalculatorTest {

    @Test
    void stepUpVolume_ratioAboveOne_matchesReferenceValue() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40StepUpVolume();

        assertEquals(IndicatorTestFixtures.VOLUME_TREND_STEP_UP_FULL,
                VolumeTrendCalculator.calculate(candles, VolumeTrendCalculator.DEFAULT_SHORT_PERIOD,
                        VolumeTrendCalculator.DEFAULT_LONG_PERIOD));
    }

    @Test
    void decliningVolume_ratioBelowOne_matchesReferenceValue() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40DecliningVolume();

        assertEquals(IndicatorTestFixtures.VOLUME_TREND_DECLINING_FULL,
                VolumeTrendCalculator.calculate(candles, VolumeTrendCalculator.DEFAULT_SHORT_PERIOD,
                        VolumeTrendCalculator.DEFAULT_LONG_PERIOD));
    }

    @Test
    void zeroVolumeWindow_returnsNullInsteadOfDividingByZero() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40ZeroVolume();

        assertNull(VolumeTrendCalculator.calculate(candles, VolumeTrendCalculator.DEFAULT_SHORT_PERIOD,
                VolumeTrendCalculator.DEFAULT_LONG_PERIOD));
    }

    @Test
    void constantVolume_ratioIsExactlyOne() {
        List<Candle> candles = IndicatorTestFixtures.candles40();

        assertEquals(IndicatorTestFixtures.VOLUME_TREND_DEGENERATE_FULL,
                VolumeTrendCalculator.calculate(candles, VolumeTrendCalculator.DEFAULT_SHORT_PERIOD,
                        VolumeTrendCalculator.DEFAULT_LONG_PERIOD));
    }

    @Test
    void fewerThanLongPeriodCandles_throws() {
        List<Candle> candles = IndicatorTestFixtures.ohlcv40First(29);

        assertThrows(IllegalArgumentException.class,
                () -> VolumeTrendCalculator.calculate(candles, VolumeTrendCalculator.DEFAULT_SHORT_PERIOD,
                        VolumeTrendCalculator.DEFAULT_LONG_PERIOD));
    }
}
