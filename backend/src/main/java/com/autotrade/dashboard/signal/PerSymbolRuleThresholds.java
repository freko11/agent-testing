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
 *
 * <p><b>E8-F1-S7 evaluated AAPL, this repo's first stock symbol, against the same tune-then-
 * validate methodology — no ship.</b> AAPL's tuning-window winner (76, beating the 75 default at
 * every checkpoint with a larger n) fails held-out confirmation, but unlike BTCUSDT/DOGEUSDT's
 * degenerate ("byte-identical candidates") failure mode, AAPL's held-out tail genuinely disagrees
 * with its own tuning window: candidate 68 — the *worst* tuning-window candidate — is the clear
 * held-out winner (e.g. max checkpoint +1.009% after-cost expectancy vs. 76's own held-out +0.304%
 * and the 75-default's +0.279%), the sharpest tuning/held-out reversal seen anywhere in this
 * backlog. Every stock ticker, including AAPL, keeps falling back to {@link
 * SignalRuleEngine.RuleThresholds#DEFAULT} — the "zero stock evidence exists" gap this class
 * previously described is now closed with *negative* evidence (a real stock sweep that didn't
 * confirm), not merely an absent one. See docs/CHANGELOG.md's E8-F1-S7 entry for the full sweep.
 *
 * <p><b>E8-F1-S8 calibrated {@code macdMinHistogramMagnitudePct} per symbol, the same
 * tune-then-validate methodology applied to a second, independent axis — and this is the first
 * story to ship a symbol with <em>two</em> non-default fields at once.</b> BTCUSDT and DOGEUSDT
 * both ship no override: BTCUSDT's tuning-window winners (macd&gt;=0.75%/1.00%, both beating the
 * magnitude-0 baseline at every checkpoint) each fail held-out confirmation specifically at the
 * MIN checkpoint, and DOGEUSDT's only tuning-window winner (macd&gt;=0.10%) fails held-out
 * confirmation completely (worse than baseline at every checkpoint there). <b>SOLUSDT ships
 * {@code macdMinHistogramMagnitudePct = 0.10}</b>, composed into its existing {@code
 * rsiOverbought = 70} entry rather than replacing it — a genuine, non-degenerate confirmation
 * (beats the magnitude-0 baseline's BUY-side after-cost expectancy at every checkpoint on both its
 * tuning window, n=186 vs. 188, and its own held-out tail, n=67 vs. 69). Unlike {@code
 * rsiOverbought} (zero measurable SELL-side effect per E8-F1-S3), this axis gates the MACD vote
 * symmetrically, so SOLUSDT's SELL-side classification changes too — checked and found to be a
 * real, positive effect at every checkpoint on both windows (see {@code
 * PerSymbolMacdHistogramMagnitudeCalibrationTest}'s class Javadoc for the full figures), not a
 * side effect that offsets the BUY-side gain. {@link SignalRuleEngine#RULE_TABLE_VERSION} bumps to
 * v5 for the resolution mechanism itself, per this story's confirmed scope, regardless of how many
 * symbols ended up with a non-default override on this axis — the same "mechanism ships, value
 * count doesn't matter" treatment E8-F1-S4 gave its own v2&rarr;v3 bump. See {@code
 * PerSymbolMacdHistogramMagnitudeCalibrationTest}'s class Javadoc and docs/CHANGELOG.md's
 * E8-F1-S8 entry for the full per-symbol sweep and figures.
 *
 * <p><b>E8-F1-S10 tried a third axis, {@code maMinSeparationPctOfPrice} — no symbol ships an
 * override.</b> {@code PerSymbolMaCrossoverSeparationCalibrationTest} swept BTCUSDT/DOGEUSDT/
 * SOLUSDT independently against this class's own tune-then-confirm design (a candidate must beat
 * the {@code separation=0} baseline at every checkpoint on a symbol's own tuning window before its
 * held-out tail is even checked). BTCUSDT's own tuning-window winners (ma&gt;=5.00%/7.00%/10.00%)
 * each fail held-out confirmation — 5.00% reverses sharply on a degenerate held-out sample (n=6),
 * and 7.00%/10.00% produce zero held-out BUY calls to confirm against at all. DOGEUSDT and SOLUSDT
 * never produce a tuning-window winner in the first place — every nonzero candidate is worse than
 * the no-filter baseline at some checkpoint on both symbols' own tuning windows. This map is
 * unchanged by this story (still only the SOLUSDT entry from E8-F1-S8); {@link
 * SignalRuleEngine#RULE_TABLE_VERSION} stays v5, since no new resolution logic actually ships. See
 * {@code PerSymbolMaCrossoverSeparationCalibrationTest}'s class Javadoc and docs/CHANGELOG.md's
 * E8-F1-S10 entry for the full per-symbol sweep and figures.
 *
 * <p><b>E8-F1-S12 swept AAPL fresh on the two axes E8-F1-S8/E8-F1-S10 already calibrated for
 * crypto - {@code macdMinHistogramMagnitudePct} and {@code maMinSeparationPctOfPrice} - neither
 * had ever been checked against a stock before. Both come back no-ship.</b> AAPL's tuning window
 * genuinely produces winners on both axes this time (unlike DOGEUSDT/SOLUSDT's own "no
 * tuning-window winner to begin with" shape) - {@code macdMinHistogramMagnitudePct} 0.50%/0.75%,
 * and {@code maMinSeparationPctOfPrice} 1.00%/2.00%/3.00%/4.00% - but every single one fails
 * held-out confirmation, most commonly at the MIN checkpoint specifically. AAPL keeps falling back
 * to {@link SignalRuleEngine.RuleThresholds#DEFAULT} on both axes, same as it already does on
 * {@code rsiOverbought} per E8-F1-S7. See {@code StockPerSymbolMacdHistogramMagnitudeCalibrationTest}'s
 * and {@code StockMaCrossoverSeparationCalibrationTest}'s own class Javadocs and
 * docs/CHANGELOG.md's E8-F1-S12 entry for the full figures. This map is unchanged by this story
 * (still only the SOLUSDT entry from E8-F1-S8).
 */
public final class PerSymbolRuleThresholds {

    private static final Map<String, SignalRuleEngine.RuleThresholds> OVERRIDES = Map.of(
            "SOLUSDT", new SignalRuleEngine.RuleThresholds(
                    SignalRuleEngine.RSI_OVERSOLD_THRESHOLD,
                    new BigDecimal("70"),
                    SignalRuleEngine.RuleThresholds.DEFAULT.volatilityExtreme(),
                    SignalRuleEngine.RuleThresholds.DEFAULT.volumeDriedUp(),
                    new BigDecimal("0.10"),
                    SignalRuleEngine.RuleThresholds.DEFAULT.maMinSeparationPctOfPrice()));

    private PerSymbolRuleThresholds() {
    }

    public static SignalRuleEngine.RuleThresholds forSymbol(String normalizedSymbol) {
        return OVERRIDES.getOrDefault(normalizedSymbol, SignalRuleEngine.RuleThresholds.DEFAULT);
    }
}
