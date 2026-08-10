package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.ticker.AssetType;

/**
 * E8-F3-S2: an engine-agnostic post-filter that suppresses a directional (BUY/SELL) call when the
 * market is in a RANGING regime — "the same MA-crossover means different things in a choppy vs.
 * trending market" (this story's AC) — by collapsing it to {@link SignalRuleId#NO_STRONG_SIGNAL},
 * the same outcome the unweighted table itself already uses for "not enough signal to call". Only
 * adds new, additive classes: does NOT add a new {@link SignalRuleId} constant, so every existing
 * consumer of that enum ({@code SignalCallEntry}, {@code OrderAuditEntry}, {@code
 * BacktestReport}'s rule lists, the frontend's TS mirror) is untouched by this story. The
 * tradeoff: {@code NO_STRONG_SIGNAL}'s rationale string ("no strong directional signal from
 * RSI/MACD/MA") is literally inaccurate for a regime-gated call — the indicators DID agree; the
 * regime override, not indicator dissent, suppressed it. Accepted because the actual "did this
 * help" evidence lives in {@code BacktestHarness}'s regime-split expectancy stats, not in which
 * enum bucket a gated call lands in.
 *
 * <p>Takes an already-computed {@link SignalRuleId} (from either {@link SignalRuleEngine#evaluate}
 * or {@code WeightedVoteRuleEngine.evaluate}) and a {@link Regime} — a pure {@code SignalRuleId x
 * Regime -> SignalRuleId} function with zero coupling to either engine's internals, so it composes
 * identically with either one.
 *
 * <p><b>{@link #applyGate} (both directions) stays unwired from production</b> — wiring it in
 * whole was conditioned on evidence from {@code RegimeCalibrationTest} showing ranging-regime
 * expectancy is consistently and materially worse than trending-regime expectancy on both BUY and
 * SELL, the same evidence-gated approach {@code WeightedVoteRuleEngine} (E8-F3-S1) took. E8-F4-S1's
 * out-of-sample validation pass covered E8-F1-S1's threshold shift and {@code
 * WeightedVoteRuleEngine}'s weights, but explicitly left this story's regime filter/{@code
 * ADX_TRENDING_THRESHOLD} out of scope. E8-F4-S2 closed that gap: {@code
 * RegimeOutOfSampleValidationTest} replayed the held-out tail of all three fixtures
 * (BTCUSDT/DOGEUSDT/SOLUSDT) and found the SELL side does hold up out-of-sample (trending beats
 * ranging on every symbol at every checkpoint) but the BUY side doesn't (ranging actually beats
 * trending on BTCUSDT and DOGEUSDT, only SOLUSDT favors trending) — since {@link #applyGate} gates
 * both directions identically with no BUY/SELL split, the combined mechanism never cleared the
 * "uniformly and materially worse across all three symbols" bar, so it stays unwired as a whole.
 * See {@code docs/CHANGELOG.md}'s E8-F4-S2 entry for the full figures.
 *
 * <p><b>{@link #applySellGate} (E8-F3-S3) is wired into production</b>, separately from {@link
 * #applyGate}: since the SELL-side evidence alone did clear that same bar on its own, this
 * SELL-only entry point is called from {@code SignalService.computeSignalWithProvenance} for
 * crypto tickers (see {@link #sellGateAppliesTo}) — BUY calls are never touched by it, regardless
 * of regime, matching E8-F4-S2's own finding that only the SELL side generalized. See {@code
 * docs/CHANGELOG.md}'s E8-F3-S3 entry for the wiring rationale and the crypto-only scoping
 * decision.
 */
public final class RegimeGatedRuleEngine {

    private RegimeGatedRuleEngine() {
    }

    /**
     * @return {@link SignalRuleId#NO_STRONG_SIGNAL} when {@code matchedRule} is a directional
     * (BUY/SELL) call and {@code regime} is {@link Regime#RANGING}; {@code matchedRule} unchanged
     * otherwise (including every HOLD-cause rule, regardless of regime — the safety gates and the
     * conflict/dissent gate already decided those, and a regime filter has nothing to add to a
     * call that's already HOLD). Unwired from production — see class Javadoc.
     */
    public static SignalRuleId applyGate(SignalRuleId matchedRule, Regime regime) {
        boolean directional = matchedRule.call() == SignalCall.BUY || matchedRule.call() == SignalCall.SELL;
        if (directional && regime == Regime.RANGING) {
            return SignalRuleId.NO_STRONG_SIGNAL;
        }
        return matchedRule;
    }

    /**
     * E8-F3-S3: the production-wired half of {@link #applyGate} — collapses {@code matchedRule} to
     * {@link SignalRuleId#NO_STRONG_SIGNAL} only when it's a SELL call in a {@link Regime#RANGING}
     * regime; BUY calls and every HOLD-cause rule pass through completely unchanged regardless of
     * regime, per E8-F4-S2's finding that only the SELL side's trending-beats-ranging expectancy
     * gap held uniformly out-of-sample across all three fixtures.
     */
    public static SignalRuleId applySellGate(SignalRuleId matchedRule, Regime regime) {
        if (matchedRule.call() == SignalCall.SELL && regime == Regime.RANGING) {
            return SignalRuleId.NO_STRONG_SIGNAL;
        }
        return matchedRule;
    }

    /**
     * @return whether {@link #applySellGate} should be consulted at all for this asset type.
     * Restricted to {@link AssetType#CRYPTO}: E8-F4-S2's out-of-sample evidence covers only
     * BTCUSDT/DOGEUSDT/SOLUSDT (all crypto) — zero stock evidence exists anywhere in this backlog,
     * so extrapolating onto stock tickers would repeat exactly the mistake {@code
     * PerSymbolRuleThresholds}'s own Javadoc refuses to make for its per-symbol RSI override.
     * Scoped to the whole asset type (not a fixed symbol allow-list, unlike {@code
     * PerSymbolRuleThresholds}): unlike an RSI threshold value (asset-specific calibrated data),
     * ADX/regime is a general trend-persistence mechanism that the evidence found works uniformly
     * across every crypto symbol tested, so it generalizes to other crypto symbols the same way the
     * rest of the rule table already does.
     */
    public static boolean sellGateAppliesTo(AssetType assetType) {
        return assetType == AssetType.CRYPTO;
    }
}
