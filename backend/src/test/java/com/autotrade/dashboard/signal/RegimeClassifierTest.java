package com.autotrade.dashboard.signal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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

    @Test
    void singleArgOverload_delegatesToGlobalThresholdExplicitly() {
        for (BigDecimal adx : List.of(new BigDecimal("0"), new BigDecimal("24.9999"),
                RegimeClassifier.ADX_TRENDING_THRESHOLD, new BigDecimal("30"), new BigDecimal("100"))) {
            assertEquals(RegimeClassifier.classify(adx, RegimeClassifier.ADX_TRENDING_THRESHOLD),
                    RegimeClassifier.classify(adx), "classify(adx) must be byte-identical to classify(adx, ADX_TRENDING_THRESHOLD)");
        }
    }

    @Test
    void customThreshold_aboveThreshold_returnsTrending() {
        assertEquals(Regime.TRENDING, RegimeClassifier.classify(new BigDecimal("41"), new BigDecimal("40")));
    }

    @Test
    void customThreshold_exactlyAtThreshold_returnsTrending() {
        assertEquals(Regime.TRENDING, RegimeClassifier.classify(new BigDecimal("40"), new BigDecimal("40")));
    }

    @Test
    void customThreshold_belowThreshold_returnsRanging() {
        assertEquals(Regime.RANGING, RegimeClassifier.classify(new BigDecimal("39.9999"), new BigDecimal("40")));
    }
}
