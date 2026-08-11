package com.autotrade.dashboard.signal;

import java.math.BigDecimal;
import java.util.Map;

/**
 * E8-F3-S4: per-symbol override of {@link RegimeClassifier#ADX_TRENDING_THRESHOLD}, keyed by
 * normalized ticker symbol — the same shape {@link PerSymbolRuleThresholds} established for its
 * per-symbol {@code rsiOverbought} override, applied here to the BUY-side regime gate instead.
 *
 * <p>Deliberately a sibling of {@link RegimeClassifier}, not a change to it: {@link
 * RegimeClassifier#classify(BigDecimal)} (no threshold argument) stays wired unconditionally to
 * the global {@link RegimeClassifier#ADX_TRENDING_THRESHOLD}, since that's the classification the
 * shipped, out-of-sample-validated SELL-side gate ({@code RegimeGatedRuleEngine#applySellGate},
 * E8-F3-S3) already depends on. This class only ever feeds {@link RegimeClassifier#classify(
 * BigDecimal, BigDecimal)}'s explicit-threshold overload, used exclusively by the BUY-side gate
 * ({@code RegimeGatedRuleEngine#applyBuyGate}) — so a per-symbol BUY threshold can never leak into
 * the SELL gate's already-validated behavior for the same symbol.
 *
 * <p>{@code forSymbol} returning the global default for an unlisted symbol is not the same as
 * {@code RegimeGatedRuleEngine#buyGateAppliesTo} returning {@code true} for that symbol — a symbol
 * can confirm the BUY gate at the global default value with no entry needed here at all. This map
 * only ever holds a symbol whose own held-out tail confirmed some *other* value beats the default;
 * see that class's Javadoc for which symbols the BUY gate is actually wired for.
 *
 * <p><b>This map is empty — no symbol ships an override.</b> {@code
 * PerSymbolAdxTrendingThresholdCalibrationTest} swept {15, 18, 20, 22, 25, 28, 30, 35, 40} against
 * each of BTCUSDT/DOGEUSDT/SOLUSDT's own tuning window, then validated any qualifying winner
 * against that same symbol's own held-out tail. All three failed independently: BTCUSDT's
 * tuning-window winners (25/28/30, where trending uniformly beat ranging with non-degenerate n)
 * all reversed on its own held-out tail (ranging won there instead); DOGEUSDT's ranging bucket beat
 * trending at every single candidate on its own tuning window, so no candidate ever qualified as a
 * winner to validate; SOLUSDT's tuning window showed the same pattern as DOGEUSDT (ranging beats
 * trending at every candidate) even though its held-out tail independently favors trending at the
 * default (per E8-F4-S2) — tuning and held-out disagree with each other before any candidate is
 * even tested. See docs/CHANGELOG.md's E8-F3-S4 entry for the full per-symbol figures.
 */
public final class PerSymbolAdxThresholds {

    private static final Map<String, BigDecimal> OVERRIDES = Map.of();

    private PerSymbolAdxThresholds() {
    }

    public static BigDecimal forSymbol(String normalizedSymbol) {
        return OVERRIDES.getOrDefault(normalizedSymbol, RegimeClassifier.ADX_TRENDING_THRESHOLD);
    }
}
