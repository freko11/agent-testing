package com.autotrade.dashboard.signal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegimeClassifierTest {

    @Test
    void adxAboveThreshold_returnsTrending() {
        assertEquals(Regime.TRENDING, RegimeClassifier.classify(new BigDecimal("30")));
    }

    @Test
    void adxExactlyAtThreshold_returnsTrending() {
        assertEquals(Regime.TRENDING, RegimeClassifier.classify(RegimeClassifier.ADX_TRENDING_THRESHOLD));
    }

    @Test
    void adxBelowThreshold_returnsRanging() {
        assertEquals(Regime.RANGING, RegimeClassifier.classify(new BigDecimal("24.9999")));
    }

    @Test
    void adxZero_returnsRanging() {
        assertEquals(Regime.RANGING, RegimeClassifier.classify(BigDecimal.ZERO));
    }
}
