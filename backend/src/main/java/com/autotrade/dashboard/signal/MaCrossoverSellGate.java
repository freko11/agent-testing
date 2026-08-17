package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.ticker.AssetType;

import java.math.BigDecimal;

/**
 * E8-F1-S11: wires {@code maMinSeparationPctOfPrice} in for SELL calls specifically, mirroring how
 * {@link RegimeGatedRuleEngine#applySellGate} (E8-F3-S3) wires the regime filter for SELL only —
 * {@code MaCrossoverSeparationCalibrationTest} (E8-F1-S6) and {@code
 * PerSymbolMaCrossoverSeparationCalibrationTest} (E8-F1-S10) each flagged a ~2.00% separation
 * threshold as improving SELL-side after-cost expectancy uniformly across all three symbols
 * (BTCUSDT/DOGEUSDT/SOLUSDT) on their own held-out tails, but both were chartered for the BUY-side
 * mismatch and left the finding unactioned. {@code SellMaCrossoverSeparationCalibrationTest}
 * (this story) confirmed it: {@code ma&gt;=2.00%} is the one candidate that beats the {@code
 * separation=0} SELL-side after-cost-expectancy baseline at every one of MIN/MID/MAX on <b>all
 * three</b> symbols' own tuning windows simultaneously — the same global, uniform-across-all-three
 * bar {@code SellMacdHistogramMagnitudeCalibrationTest} (E8-F1-S9) used but failed to clear — and
 * that candidate goes on to confirm on all three symbols' own held-out tails too (e.g. BTCUSDT max
 * +1.213% vs. baseline +0.999%; DOGEUSDT max +1.612% vs. +1.206%; SOLUSDT max +1.506% vs.
 * +0.844%). See docs/CHANGELOG.md's E8-F1-S11 entry for the full figures.
 *
 * <p>Unlike {@link RegimeGatedRuleEngine#applySellGate} (whose {@link Regime} input is orthogonal
 * to {@link SignalRuleEngine#evaluate}'s own vote computation), {@code maMinSeparationPctOfPrice}
 * gates {@link SignalRuleEngine#computeVotes}'s {@code maBullish}/{@code maBearish} reads directly
 * and symmetrically — the same "no way to make it BUY-only without an out-of-scope {@code
 * evaluate} signature change" constraint {@code PerSymbolMacdHistogramMagnitudeCalibrationTest}'s
 * SOLUSDT override (E8-F1-S8) hit on the MACD axis. So "SELL-only classification" here means:
 * re-run {@link SignalRuleEngine#evaluate} with a second {@link SignalRuleEngine.RuleThresholds}
 * equal to the caller's base thresholds except {@code maMinSeparationPctOfPrice} raised to {@link
 * #SELL_MIN_SEPARATION_PCT_OF_PRICE}, and only keep {@code matchedRule} if that stricter
 * re-evaluation is <b>still</b> a SELL call — otherwise collapse to {@link
 * SignalRuleId#NO_STRONG_SIGNAL}, the same collapse {@code applySellGate} uses. Raising the
 * threshold can only ever remove a bearish MA vote (never add one), so the stricter re-evaluation
 * can only downgrade or preserve a SELL call, never flip a non-SELL {@code matchedRule} into one —
 * {@link #applySellGate} only needs to act when {@code matchedRule} is already a SELL call, and
 * BUY calls (and every HOLD-cause rule) pass through completely untouched, regardless of what the
 * stricter re-evaluation would have said. BUY classification itself is unaffected by this class
 * entirely — it stays on whatever {@code maMinSeparationPctOfPrice} the caller's base thresholds
 * already resolve (still 0 for every symbol, since {@code PerSymbolMaCrossoverSeparationCalibrationTest}
 * (E8-F1-S10) shipped no per-symbol BUY-side override).
 *
 * <p>{@code SignalService.computeSignalWithProvenance} calls {@link #applySellGate} after {@link
 * RegimeGatedRuleEngine#applySellGate}, for crypto tickers only (see {@link #sellGateAppliesTo}) —
 * the two SELL-only gates compose independently (each only ever downgrades an already-resolved
 * SELL call to {@code NO_STRONG_SIGNAL}, never the reverse), so applying both in sequence is
 * equivalent regardless of order. {@link SignalRuleEngine#RULE_TABLE_VERSION} bumps v5&rarr;v6
 * since, like E8-F3-S3, this changes a real resolved {@link SignalRuleId} for a real input class (a
 * crypto SELL call whose MA-crossover separation falls short of 2.00%).
 */
public final class MaCrossoverSellGate {

    /** The one candidate {@code SellMaCrossoverSeparationCalibrationTest} found clears the
     * uniform-across-all-three-symbols ship bar on both the tuning windows and the held-out tails —
     * see class Javadoc. */
    public static final BigDecimal SELL_MIN_SEPARATION_PCT_OF_PRICE = new BigDecimal("2.00");

    private MaCrossoverSellGate() {
    }

    /**
     * @return {@link SignalRuleId#NO_STRONG_SIGNAL} when {@code matchedRule} is a SELL call whose
     * MA-crossover separation does not clear {@link #SELL_MIN_SEPARATION_PCT_OF_PRICE} once
     * re-evaluated under the stricter threshold; {@code matchedRule} unchanged otherwise (including
     * every BUY call and every HOLD-cause rule, regardless of separation).
     */
    public static SignalRuleId applySellGate(SignalRuleId matchedRule, BigDecimal rsi, MacdResult macd,
                                              MovingAverageResult movingAverage, BigDecimal volatility,
                                              BigDecimal volumeTrend, SignalRuleEngine.RuleThresholds baseThresholds) {
        if (matchedRule.call() != SignalCall.SELL) {
            return matchedRule;
        }
        SignalRuleEngine.RuleThresholds sellThresholds = new SignalRuleEngine.RuleThresholds(
                baseThresholds.rsiOversold(), baseThresholds.rsiOverbought(), baseThresholds.volatilityExtreme(),
                baseThresholds.volumeDriedUp(), baseThresholds.macdMinHistogramMagnitudePct(),
                SELL_MIN_SEPARATION_PCT_OF_PRICE);
        SignalRuleId sellResolution = SignalRuleEngine.evaluate(rsi, macd, movingAverage, volatility, volumeTrend, sellThresholds);
        return sellResolution.call() == SignalCall.SELL ? sellResolution : SignalRuleId.NO_STRONG_SIGNAL;
    }

    /**
     * @return whether {@link #applySellGate} should be consulted at all for this asset type.
     * Restricted to {@link AssetType#CRYPTO}, the same restriction {@link
     * RegimeGatedRuleEngine#sellGateAppliesTo} uses and for the same reason: {@code
     * SellMaCrossoverSeparationCalibrationTest}'s evidence covers only BTCUSDT/DOGEUSDT/SOLUSDT
     * (all crypto) — zero stock evidence existed for the MA-crossover separation axis specifically
     * when this gate first shipped (E8-F1-S7's stock fixture only evaluated the per-symbol RSI
     * override and the regime gate), so extrapolating onto stock tickers would have repeated the
     * same mistake {@code PerSymbolRuleThresholds} and {@code RegimeGatedRuleEngine} both already
     * guard against.
     *
     * <p><b>E8-F1-S12 closed that gap and found active contradicting evidence, not merely absent
     * evidence.</b> {@code StockMaCrossoverSeparationCalibrationTest} checked whether {@link
     * #SELL_MIN_SEPARATION_PCT_OF_PRICE} (2.00%) actually improves AAPL's own SELL-side after-cost
     * expectancy the way it does for all three crypto symbols — it does not. The shipped value
     * makes AAPL's SELL-side expectancy uniformly <em>worse</em> at all six checkpoints checked
     * (three on AAPL's own tuning window, three on its held-out tail), a direct contradiction of
     * the crypto-wide finding this gate's scoping already relies on, not a mixed or marginal
     * result. This method's crypto-only scoping is unchanged by this finding — it was already
     * crypto-only — but the reasoning behind it is now backed by a real, checked stock result
     * rather than an absent one. See docs/CHANGELOG.md's E8-F1-S12 entry for the full figures.
     */
    public static boolean sellGateAppliesTo(AssetType assetType) {
        return assetType == AssetType.CRYPTO;
    }
}
