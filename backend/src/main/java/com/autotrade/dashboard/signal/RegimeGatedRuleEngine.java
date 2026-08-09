package com.autotrade.dashboard.signal;

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
 * regime override, not indicator dissent, suppressed it. Accepted because this class is unwired
 * from production (see below) and the actual "did this help" evidence lives in {@code
 * BacktestHarness}'s regime-split expectancy stats, not in which enum bucket a gated call lands
 * in.
 *
 * <p>Takes an already-computed {@link SignalRuleId} (from either {@link SignalRuleEngine#evaluate}
 * or {@code WeightedVoteRuleEngine.evaluate}) and a {@link Regime} — a pure {@code SignalRuleId x
 * Regime -> SignalRuleId} function with zero coupling to either engine's internals, so it composes
 * identically with either one.
 *
 * <p><b>Deliberately not wired into production.</b> {@code SignalService}/{@code OrderService}
 * still call {@link SignalRuleEngine#evaluate} directly, unfiltered — wiring this in was
 * conditioned on evidence from {@code RegimeCalibrationTest} showing ranging-regime expectancy is
 * consistently and materially worse than trending-regime expectancy, the same evidence-gated
 * approach {@code WeightedVoteRuleEngine} (E8-F3-S1) took. E8-F4-S1's out-of-sample validation
 * pass covered E8-F1-S1's threshold shift and {@code WeightedVoteRuleEngine}'s weights, but
 * explicitly left this story's regime filter/{@code ADX_TRENDING_THRESHOLD} out of scope. E8-F4-S2
 * closed that gap: {@code RegimeOutOfSampleValidationTest} replayed the held-out tail of all three
 * fixtures (BTCUSDT/DOGEUSDT/SOLUSDT) and found the SELL side does hold up out-of-sample (trending
 * beats ranging on every symbol at every checkpoint) but the BUY side doesn't (ranging actually
 * beats trending on BTCUSDT and DOGEUSDT, only SOLUSDT favors trending) — since {@link #applyGate}
 * gates both directions identically with no BUY/SELL split, this doesn't clear the "uniformly and
 * materially worse across all three symbols" bar the wiring decision was conditioned on, so this
 * class remains unwired. See {@code docs/CHANGELOG.md}'s E8-F4-S2 entry for the full figures.
 */
public final class RegimeGatedRuleEngine {

    private RegimeGatedRuleEngine() {
    }

    /**
     * @return {@link SignalRuleId#NO_STRONG_SIGNAL} when {@code matchedRule} is a directional
     * (BUY/SELL) call and {@code regime} is {@link Regime#RANGING}; {@code matchedRule} unchanged
     * otherwise (including every HOLD-cause rule, regardless of regime — the safety gates and the
     * conflict/dissent gate already decided those, and a regime filter has nothing to add to a
     * call that's already HOLD).
     */
    public static SignalRuleId applyGate(SignalRuleId matchedRule, Regime regime) {
        boolean directional = matchedRule.call() == SignalCall.BUY || matchedRule.call() == SignalCall.SELL;
        if (directional && regime == Regime.RANGING) {
            return SignalRuleId.NO_STRONG_SIGNAL;
        }
        return matchedRule;
    }
}
