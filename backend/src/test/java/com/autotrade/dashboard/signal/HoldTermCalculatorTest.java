package com.autotrade.dashboard.signal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** One test per {@link HoldTermRule} branch, volatility-band boundaries, and the HOLD-call null contract. */
class HoldTermCalculatorTest {

    @Test
    void strongTrend_lowVolatility_returnsFiveToFifteenDays() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_UNANIMOUS, new BigDecimal("1.5"));
        assertEquals(5, holdTerm.minDays());
        assertEquals(15, holdTerm.maxDays());
        assertEquals("5-15 days", holdTerm.label());
    }

    @Test
    void strongTrend_mediumVolatility_returnsThreeToTenDays() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BEARISH_UNANIMOUS, new BigDecimal("3.0"));
        assertEquals(3, holdTerm.minDays());
        assertEquals(10, holdTerm.maxDays());
        assertEquals("3-10 days", holdTerm.label());
    }

    @Test
    void strongTrend_highVolatility_returnsTwoToSixDays() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_UNANIMOUS, new BigDecimal("6.0"));
        assertEquals(2, holdTerm.minDays());
        assertEquals(6, holdTerm.maxDays());
        assertEquals("2-6 days", holdTerm.label());
    }

    @Test
    void moderateTrend_lowVolatility_returnsThreeToTenDays() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_MAJORITY, new BigDecimal("1.0"));
        assertEquals(3, holdTerm.minDays());
        assertEquals(10, holdTerm.maxDays());
        assertEquals("3-10 days", holdTerm.label());
    }

    @Test
    void moderateTrend_mediumVolatility_returnsTwoToSevenDays() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BEARISH_MAJORITY, new BigDecimal("4.0"));
        assertEquals(2, holdTerm.minDays());
        assertEquals(7, holdTerm.maxDays());
        assertEquals("2-7 days", holdTerm.label());
    }

    @Test
    void moderateTrend_highVolatility_returnsOneToFourDays() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_MAJORITY, new BigDecimal("7.0"));
        assertEquals(1, holdTerm.minDays());
        assertEquals(4, holdTerm.maxDays());
        assertEquals("1-4 days", holdTerm.label());
    }

    @Test
    void volatilityExactlyAtLowMax_classifiesAsMedium() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_UNANIMOUS,
                HoldTermCalculator.VOLATILITY_LOW_MAX);
        assertEquals(3, holdTerm.minDays());
        assertEquals(10, holdTerm.maxDays());
    }

    @Test
    void volatilityExactlyAtMediumMax_classifiesAsHigh() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_UNANIMOUS,
                HoldTermCalculator.VOLATILITY_MEDIUM_MAX);
        assertEquals(2, holdTerm.minDays());
        assertEquals(6, holdTerm.maxDays());
    }

    @Test
    void tableVersion_isStampedOnResult() {
        HoldTerm holdTerm = HoldTermCalculator.calculate(SignalRuleId.BULLISH_UNANIMOUS, new BigDecimal("1.0"));
        assertEquals(HoldTermCalculator.HOLD_TERM_TABLE_VERSION, holdTerm.tableVersion());
    }

    @Test
    void noVolumeData_holdCall_returnsNull() {
        assertNull(HoldTermCalculator.calculate(SignalRuleId.NO_VOLUME_DATA, new BigDecimal("1.0")));
    }

    @Test
    void volumeDriedUp_holdCall_returnsNull() {
        assertNull(HoldTermCalculator.calculate(SignalRuleId.VOLUME_DRIED_UP, new BigDecimal("1.0")));
    }

    @Test
    void volatilityTooExtreme_holdCall_returnsNull() {
        assertNull(HoldTermCalculator.calculate(SignalRuleId.VOLATILITY_TOO_EXTREME, new BigDecimal("9.0")));
    }

    @Test
    void conflictingSignals_holdCall_returnsNull() {
        assertNull(HoldTermCalculator.calculate(SignalRuleId.CONFLICTING_SIGNALS, new BigDecimal("1.0")));
    }

    @Test
    void noStrongSignal_holdCall_returnsNull() {
        assertNull(HoldTermCalculator.calculate(SignalRuleId.NO_STRONG_SIGNAL, new BigDecimal("1.0")));
    }
}
