package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MacdCalculatorTest {

    @Test
    void fullSeries_matchesReferenceValues() {
        MacdResult result = MacdCalculator.calculate(IndicatorTestFixtures.candles40(),
                MacdCalculator.DEFAULT_FAST_PERIOD, MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD);

        assertEquals(IndicatorTestFixtures.MACD_LINE_FULL, result.line());
        assertEquals(IndicatorTestFixtures.MACD_SIGNAL_FULL, result.signal());
        assertEquals(IndicatorTestFixtures.MACD_HISTOGRAM_FULL, result.histogram());
    }

    @Test
    void minimumCandleCount_34_matchesSignalSeedBoundaryReferenceValues() {
        MacdResult result = MacdCalculator.calculate(IndicatorTestFixtures.candlesFirst(34),
                MacdCalculator.DEFAULT_FAST_PERIOD, MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD);

        assertEquals(IndicatorTestFixtures.MACD_LINE_34, result.line());
        assertEquals(IndicatorTestFixtures.MACD_SIGNAL_34, result.signal());
        assertEquals(IndicatorTestFixtures.MACD_HISTOGRAM_34, result.histogram());
    }

    @Test
    void fewerThanMinimumCandles_throws() {
        List<Candle> tooFew = IndicatorTestFixtures.candlesFirst(33);

        assertThrows(IllegalArgumentException.class,
                () -> MacdCalculator.calculate(tooFew,
                        MacdCalculator.DEFAULT_FAST_PERIOD, MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD));
    }
}
