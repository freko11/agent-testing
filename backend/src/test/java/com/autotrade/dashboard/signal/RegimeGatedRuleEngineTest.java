package com.autotrade.dashboard.signal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E8-F3-S2: pure enum-in/enum-out coverage of {@link RegimeGatedRuleEngine#applyGate} — mirrors
 * {@code WeightedVoteRuleEngineTest}'s isolated-mechanism-unit-test style.
 */
class RegimeGatedRuleEngineTest {

    @Test
    void buyInRangingRegime_suppressedToNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                RegimeGatedRuleEngine.applyGate(SignalRuleId.BULLISH_UNANIMOUS, Regime.RANGING));
    }

    @Test
    void sellInRangingRegime_suppressedToNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                RegimeGatedRuleEngine.applyGate(SignalRuleId.BEARISH_MAJORITY, Regime.RANGING));
    }

    @Test
    void buyInTrendingRegime_unchanged() {
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS,
                RegimeGatedRuleEngine.applyGate(SignalRuleId.BULLISH_UNANIMOUS, Regime.TRENDING));
    }

    @Test
    void sellInTrendingRegime_unchanged() {
        assertEquals(SignalRuleId.BEARISH_MAJORITY,
                RegimeGatedRuleEngine.applyGate(SignalRuleId.BEARISH_MAJORITY, Regime.TRENDING));
    }

    @Test
    void holdCauseRule_unchangedRegardlessOfRegime() {
        for (Regime regime : Regime.values()) {
            assertEquals(SignalRuleId.VOLATILITY_TOO_EXTREME,
                    RegimeGatedRuleEngine.applyGate(SignalRuleId.VOLATILITY_TOO_EXTREME, regime));
            assertEquals(SignalRuleId.CONFLICTING_SIGNALS,
                    RegimeGatedRuleEngine.applyGate(SignalRuleId.CONFLICTING_SIGNALS, regime));
            assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                    RegimeGatedRuleEngine.applyGate(SignalRuleId.NO_STRONG_SIGNAL, regime));
        }
    }
}
