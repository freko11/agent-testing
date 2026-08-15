package com.autotrade.dashboard.signal;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E8-F1-S8: dedicated coverage for {@link PerSymbolRuleThresholds}, which previously had no test
 * file of its own — every prior E8-F1 story exercised it only indirectly, via a calibration test's
 * own structural assertions or a real {@code SignalServiceTest} case. Mirrors {@code
 * PerSymbolAdxThresholdsTest}'s unlisted-symbol/stock-symbol fallback shape, plus a case proving
 * the composition logic {@link PerSymbolRuleThresholds#OVERRIDES}'s Javadoc describes actually
 * works: SOLUSDT's real entry now carries two independently-calibrated non-default fields at once
 * (E8-F1-S4's {@code rsiOverbought} and this story's own {@code macdMinHistogramMagnitudePct}),
 * so that composition is pinned down directly against the real map rather than a constructed
 * stand-in.
 */
class PerSymbolRuleThresholdsTest {

    @Test
    void unlistedCryptoSymbol_fallsBackToGlobalDefault() {
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT, PerSymbolRuleThresholds.forSymbol("BTCUSDT"));
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT, PerSymbolRuleThresholds.forSymbol("DOGEUSDT"));
    }

    @Test
    void stockSymbol_fallsBackToGlobalDefault() {
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT, PerSymbolRuleThresholds.forSymbol("AAPL"));
    }

    @Test
    void unrecognizedSymbol_fallsBackToGlobalDefault() {
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT, PerSymbolRuleThresholds.forSymbol("NOTASYMBOL"));
    }

    /** Pins down the composition {@link PerSymbolRuleThresholds#OVERRIDES}'s Javadoc describes:
     * SOLUSDT's entry carries both its E8-F1-S4 {@code rsiOverbought} override and its E8-F1-S8
     * {@code macdMinHistogramMagnitudePct} override at once, with every other field still at
     * {@link SignalRuleEngine.RuleThresholds#DEFAULT}'s value — proving neither calibration
     * clobbered the other when they were composed into one record. */
    @Test
    void solusdt_composesBothIndependentlyCalibratedOverrides() {
        SignalRuleEngine.RuleThresholds resolved = PerSymbolRuleThresholds.forSymbol("SOLUSDT");

        assertEquals(new BigDecimal("70"), resolved.rsiOverbought(), "E8-F1-S4's override must survive composition");
        assertEquals(new BigDecimal("0.10"), resolved.macdMinHistogramMagnitudePct(),
                "E8-F1-S8's override must survive composition");

        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT.rsiOversold(), resolved.rsiOversold(),
                "rsiOversold has no per-symbol evidence and must stay at the global default");
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT.volatilityExtreme(), resolved.volatilityExtreme(),
                "volatilityExtreme has no per-symbol evidence and must stay at the global default");
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT.volumeDriedUp(), resolved.volumeDriedUp(),
                "volumeDriedUp has no per-symbol evidence and must stay at the global default");
        assertEquals(SignalRuleEngine.RuleThresholds.DEFAULT.maMinSeparationPctOfPrice(), resolved.maMinSeparationPctOfPrice(),
                "maMinSeparationPctOfPrice has no per-symbol evidence and must stay at the global default");
    }
}
