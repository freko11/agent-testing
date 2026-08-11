package com.autotrade.dashboard.signal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerSymbolAdxThresholdsTest {

    @Test
    void unlistedCryptoSymbol_fallsBackToGlobalDefault() {
        assertEquals(RegimeClassifier.ADX_TRENDING_THRESHOLD, PerSymbolAdxThresholds.forSymbol("BTCUSDT"));
        assertEquals(RegimeClassifier.ADX_TRENDING_THRESHOLD, PerSymbolAdxThresholds.forSymbol("DOGEUSDT"));
        assertEquals(RegimeClassifier.ADX_TRENDING_THRESHOLD, PerSymbolAdxThresholds.forSymbol("SOLUSDT"));
    }

    @Test
    void stockSymbol_fallsBackToGlobalDefault() {
        assertEquals(RegimeClassifier.ADX_TRENDING_THRESHOLD, PerSymbolAdxThresholds.forSymbol("AAPL"));
    }
}
