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
 */
public final class RegimeClassifier {

    public static final BigDecimal ADX_TRENDING_THRESHOLD = new BigDecimal("25");

    private RegimeClassifier() {
    }

    public static Regime classify(BigDecimal adx) {
        return adx.compareTo(ADX_TRENDING_THRESHOLD) >= 0 ? Regime.TRENDING : Regime.RANGING;
    }
}
