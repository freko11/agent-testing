package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.ticker.AssetType;

import java.util.Set;

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
     *
     * <p><b>E8-F1-S7 checked this restriction against AAPL, this repo's first stock symbol — the
     * crypto-only scope stays, and the evidence now actively argues against widening it.</b> On
     * AAPL's held-out tail, SELL-side ranging expectancy beats trending at every checkpoint
     * (e.g. max: ranging +1.518% after-cost vs. trending +0.800%, on a larger n too — 41 vs. 8) —
     * the opposite of the uniform trending-beats-ranging pattern all three crypto symbols showed.
     * One stock symbol contradicting the pattern isn't proof stocks always invert it, but it is
     * exactly the kind of asset-class divergence this restriction exists to guard against, and it
     * rules out ever widening {@code sellGateAppliesTo} to {@link AssetType#STOCK} off this
     * evidence. See docs/CHANGELOG.md's E8-F1-S7 entry for the full figures.
     */
    public static boolean sellGateAppliesTo(AssetType assetType) {
        return assetType == AssetType.CRYPTO;
    }

    /**
     * E8-F3-S4: the per-symbol BUY-side counterpart to {@link #applySellGate} — collapses {@code
     * matchedRule} to {@link SignalRuleId#NO_STRONG_SIGNAL} only when it's a BUY call in a {@link
     * Regime#RANGING} regime; SELL calls and every HOLD-cause rule pass through completely
     * unchanged regardless of regime. Unlike {@link #applySellGate}, {@code regime} here must
     * already be classified against a per-symbol threshold ({@code PerSymbolAdxThresholds}), not
     * the global {@link RegimeClassifier#ADX_TRENDING_THRESHOLD} — see {@link #buyGateAppliesTo}
     * for which symbols this gate is wired for at all.
     */
    public static SignalRuleId applyBuyGate(SignalRuleId matchedRule, Regime regime) {
        if (matchedRule.call() == SignalCall.BUY && regime == Regime.RANGING) {
            return SignalRuleId.NO_STRONG_SIGNAL;
        }
        return matchedRule;
    }

    /**
     * E8-F3-S4: symbols confirmed here have a BUY-side per-symbol trending-threshold override
     * (see {@code PerSymbolAdxTrendingThresholdCalibrationTest}'s class Javadoc for the sweep
     * methodology and docs/CHANGELOG.md's E8-F3-S4 entry for the actual per-symbol figures)
     * that cleared the same tune-then-validate-on-own-held-out-tail bar {@code
     * PerSymbolRuleThresholds}'s per-symbol RSI override used. Deliberately a fixed allow-list, not
     * an {@link AssetType} check like {@link #sellGateAppliesTo}: unlike the SELL gate (evidence
     * held uniformly across every crypto symbol tested), the BUY-side regime effect is
     * fixture-dependent — E8-F4-S2 found it favors ranging on some symbols and trending on others —
     * so wiring is conditioned on each symbol's own confirmed evidence, not generalized to the
     * whole asset class. A symbol can confirm at the global default value with no {@code
     * PerSymbolAdxThresholds} entry at all — membership here and having a non-default override are
     * independent facts.
     *
     * <p><b>This set is empty — no symbol confirms, so the BUY gate stays unwired for every
     * symbol.</b> See {@code PerSymbolAdxThresholds}' Javadoc for why: BTCUSDT's tuning-window
     * winners reversed on its own held-out tail, and DOGEUSDT/SOLUSDT's tuning windows never
     * produced a qualifying winner in the first place (ranging beat trending at every swept
     * candidate on both). {@link #applyGate} (both directions, still unwired) and this per-symbol
     * BUY gate now cover every mechanism E8-F3/E8-F4 tried for the BUY-side regime effect; per
     * E8-F4-S2's own finding, it remains fixture-dependent in a way no threshold value — global or
     * per-symbol — resolves.
     */
    public static boolean buyGateAppliesTo(String normalizedSymbol) {
        return BUY_GATE_CONFIRMED_SYMBOLS.contains(normalizedSymbol);
    }

    private static final Set<String> BUY_GATE_CONFIRMED_SYMBOLS = Set.of();
}
