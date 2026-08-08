package com.autotrade.dashboard.signal;

import java.math.BigDecimal;
import java.util.Map;

/**
 * E8-F1-S4: per-symbol override of {@link SignalRuleEngine.RuleThresholds#rsiOverbought}, keyed
 * by normalized ticker symbol (trim+uppercase — {@code Ticker.getSymbol()}'s persisted form, per
 * {@code TickerService.normalize}) rather than by {@code AssetType}. Keying by asset type would
 * erase exactly the distinction the evidence found: all three symbols calibrated here are crypto
 * and still disagree with each other on the right value (see
 * {@code PerSymbolRsiOverboughtCalibrationTest}'s class Javadoc for the sweep methodology and
 * docs/CHANGELOG.md's E8-F1-S4 entry for the actual per-symbol figures).
 *
 * <p>Compiled Java constants, not Spring config ({@code application.properties}) — same treatment
 * as every other E8 threshold constant. These are evidence-derived, versioned values that feed the
 * audit trail indirectly (via {@link SignalRuleEngine#RULE_TABLE_VERSION}), not an ops-tunable
 * runtime knob.
 *
 * <p>Only {@code rsiOverbought} varies per symbol; {@code rsiOversold}/{@code volatilityExtreme}/
 * {@code volumeDriedUp} stay at {@link SignalRuleEngine.RuleThresholds#DEFAULT}'s values for every
 * symbol, including every symbol in this map — E8-F1-S2 already found {@code rsiOversold} has no
 * measurable BUY-side effect, so it was never independently re-swept per symbol either, per this
 * story's confirmed scope.
 *
 * <p>Every stock ticker, and every crypto symbol outside this map, falls back to {@link
 * SignalRuleEngine.RuleThresholds#DEFAULT} unconditionally — zero stock evidence exists in this
 * backlog, and extrapolating a crypto-tuned value onto an untested symbol would be exactly the
 * kind of unvalidated change this epic exists to avoid.
 *
 * <p><b>Only SOLUSDT ships an override (25/70).</b> BTCUSDT and DOGEUSDT's own tuning-window
 * winners (both 76) failed their own held-out-tail confirmation — not because the held-out data
 * contradicted them, but because it couldn't: candidates 71 through 76 produce byte-identical
 * classification on both symbols' own held-out tails (no held-out candle's RSI falls in a range
 * that distinguishes those threshold values), so there is zero held-out evidence the tuning-window
 * gain generalizes for either symbol. SOLUSDT's own winner (70 — the pre-tuning global value) is a
 * genuine, non-degenerate confirmation: it beats the current global default (75) at every one of
 * MIN/MID/MAX on SOLUSDT's own held-out tail, with a comparable scored {@code n} (67 vs. 69). See
 * docs/CHANGELOG.md's E8-F1-S4 entry for the full per-symbol sweep and figures.
 */
public final class PerSymbolRuleThresholds {

    private static final Map<String, SignalRuleEngine.RuleThresholds> OVERRIDES = Map.of(
            "SOLUSDT", new SignalRuleEngine.RuleThresholds(
                    SignalRuleEngine.RSI_OVERSOLD_THRESHOLD,
                    new BigDecimal("70"),
                    SignalRuleEngine.RuleThresholds.DEFAULT.volatilityExtreme(),
                    SignalRuleEngine.RuleThresholds.DEFAULT.volumeDriedUp()));

    private PerSymbolRuleThresholds() {
    }

    public static SignalRuleEngine.RuleThresholds forSymbol(String normalizedSymbol) {
        return OVERRIDES.getOrDefault(normalizedSymbol, SignalRuleEngine.RuleThresholds.DEFAULT);
    }
}
