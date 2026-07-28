package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovingAverageCrossoverCalculatorTest {

    @Test
    void fullSeries_matchesReferenceValues() {
        MovingAverageResult result = MovingAverageCrossoverCalculator.calculate(IndicatorTestFixtures.candles40(),
                MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);

        assertEquals(IndicatorTestFixtures.SMA_10_FULL, result.shortMa());
        assertEquals(IndicatorTestFixtures.SMA_30_FULL, result.longMa());
        assertEquals(MovingAverageRelation.SHORT_ABOVE_LONG, result.relation());
    }

    @Test
    void strictlyDecreasingCloses_shortBelowLong() {
        MovingAverageResult result = MovingAverageCrossoverCalculator.calculate(
                IndicatorTestFixtures.bearishCrossoverCandles(),
                MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);

        assertEquals(MovingAverageRelation.SHORT_BELOW_LONG, result.relation());
    }

    @Test
    void fewerThanLongPeriodCandles_throws() {
        List<Candle> tooFew = IndicatorTestFixtures.candlesFirst(29);

        assertThrows(IllegalArgumentException.class,
                () -> MovingAverageCrossoverCalculator.calculate(tooFew,
                        MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD));
    }
}
