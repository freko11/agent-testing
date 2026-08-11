package com.autotrade.dashboard.signal;

import java.math.BigDecimal;

/**
 * Classifies a precomputed ADX reading ({@link com.autotrade.dashboard.indicator.AdxCalculator})
 * into a coarse {@link Regime}, mirroring {@link HoldTermCalculator#calculate}'s
 * classify-an-already-computed-indicator-value shape (a tiny pure classifier, not a raw-candle
 * consumer).
 *
 * <p>{@link #ADX_TRENDING_THRESHOLD} (25) is the common ADX rule-of-thumb (ADX&gt;25 trending,
 * &lt;20 ranging/weak-trend, with 20-25 an ambiguous band) — a deliberately uncalibrated
 * placeholder, the same treatment as {@code BacktestConfig.TAKE_PROFIT_PCT}/{@code
 * STOP_LOSS_PCT}, resolving the ambiguous band to RANGING (the more conservative failure mode: an
 * uncertain regime is treated as one where the directional vote should be gated, not trusted by
 * default), pending a future calibration sweep akin to E8-F1-S1's threshold sweep.
 *
 * <p>E8-F3-S4: {@link #classify(BigDecimal, BigDecimal)} takes an explicit threshold instead of
 * the global default, so a per-symbol BUY-side threshold ({@code PerSymbolAdxThresholds}) can be
 * applied without touching the global constant that still feeds the shipped, out-of-sample-
 * validated SELL-side gate ({@code RegimeGatedRuleEngine#applySellGate}, via {@link
 * #classify(BigDecimal)} unconditionally). Keeping these as two separate call sites — never a
 * single shared {@code Regime} value reclassified once per symbol — is deliberate: it stops a
 * BUY-tuned per-symbol threshold from silently reshaping the SELL gate's already-validated
 * behavior for the same symbol.
 */
public final class RegimeClassifier {

    public static final BigDecimal ADX_TRENDING_THRESHOLD = new BigDecimal("25");

    private RegimeClassifier() {
    }

    public static Regime classify(BigDecimal adx) {
        return classify(adx, ADX_TRENDING_THRESHOLD);
    }

    public static Regime classify(BigDecimal adx, BigDecimal threshold) {
        return adx.compareTo(threshold) >= 0 ? Regime.TRENDING : Regime.RANGING;
    }
}
