package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S2: pure enum-in/enum-out coverage of {@link RegimeGatedRuleEngine#applyGate} — mirrors
 * {@code WeightedVoteRuleEngineTest}'s isolated-mechanism-unit-test style. E8-F3-S3 adds coverage
 * of the production-wired {@link RegimeGatedRuleEngine#applySellGate}/{@link
 * RegimeGatedRuleEngine#sellGateAppliesTo} pair below.
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

    @Test
    void sellInRangingRegime_forSellOnlyGate_suppressedToNoStrongSignal() {
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                RegimeGatedRuleEngine.applySellGate(SignalRuleId.BEARISH_MAJORITY, Regime.RANGING));
    }

    @Test
    void sellInTrendingRegime_forSellOnlyGate_unchanged() {
        assertEquals(SignalRuleId.BEARISH_MAJORITY,
                RegimeGatedRuleEngine.applySellGate(SignalRuleId.BEARISH_MAJORITY, Regime.TRENDING));
    }

    @Test
    void buyInRangingRegime_forSellOnlyGate_unaffected() {
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS,
                RegimeGatedRuleEngine.applySellGate(SignalRuleId.BULLISH_UNANIMOUS, Regime.RANGING));
    }

    @Test
    void holdCauseRule_forSellOnlyGate_unchangedRegardlessOfRegime() {
        for (Regime regime : Regime.values()) {
            assertEquals(SignalRuleId.VOLATILITY_TOO_EXTREME,
                    RegimeGatedRuleEngine.applySellGate(SignalRuleId.VOLATILITY_TOO_EXTREME, regime));
            assertEquals(SignalRuleId.CONFLICTING_SIGNALS,
                    RegimeGatedRuleEngine.applySellGate(SignalRuleId.CONFLICTING_SIGNALS, regime));
            assertEquals(SignalRuleId.NO_STRONG_SIGNAL,
                    RegimeGatedRuleEngine.applySellGate(SignalRuleId.NO_STRONG_SIGNAL, regime));
        }
    }

    @Test
    void sellGateAppliesTo_crypto_true() {
        assertTrue(RegimeGatedRuleEngine.sellGateAppliesTo(AssetType.CRYPTO));
    }

    @Test
    void sellGateAppliesTo_stock_false() {
        assertFalse(RegimeGatedRuleEngine.sellGateAppliesTo(AssetType.STOCK));
    }
}
