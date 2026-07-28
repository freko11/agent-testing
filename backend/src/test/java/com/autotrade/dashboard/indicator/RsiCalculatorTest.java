package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RsiCalculatorTest {

    @Test
    void fullSeries_matchesReferenceValue() {
        BigDecimal rsi = RsiCalculator.calculate(IndicatorTestFixtures.candles40(), RsiCalculator.DEFAULT_PERIOD);

        assertEquals(IndicatorTestFixtures.RSI_14_FULL, rsi);
    }

    @Test
    void allGains_isExactly100() {
        BigDecimal rsi = RsiCalculator.calculate(IndicatorTestFixtures.allGainsCandles(20), RsiCalculator.DEFAULT_PERIOD);

        assertEquals(new BigDecimal("100.0000"), rsi);
    }

    @Test
    void flatPrice_isNeutral50_notAllGains100() {
        BigDecimal rsi = RsiCalculator.calculate(IndicatorTestFixtures.flatCandles(20), RsiCalculator.DEFAULT_PERIOD);

        assertEquals(new BigDecimal("50.0000"), rsi);
    }

    @Test
    void fewerThanPeriodPlusOneCandles_throws() {
        List<Candle> tooFew = IndicatorTestFixtures.candlesFirst(14);

        assertThrows(IllegalArgumentException.class,
                () -> RsiCalculator.calculate(tooFew, RsiCalculator.DEFAULT_PERIOD));
    }
}
