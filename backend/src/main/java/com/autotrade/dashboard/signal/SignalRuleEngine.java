package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;

import java.math.BigDecimal;

/**
 * Deterministic Buy/Sell/Hold rule table (E2-F3-S1): three safety gates (null/dried-up
 * volume, extreme volatility) run first and can only ever force HOLD, then a 2-of-3
 * directional vote across RSI/MACD/MA-crossover with no dissent allowed decides BUY/SELL.
 * Rules are evaluated in {@link SignalRuleId} declaration order; the first match wins.
 *
 * <p>RSI 25/75 and the two gate thresholds were backtest-calibrated (E8-F1-S1) by sweeping
 * candidates through {@code BacktestHarness} against the checked-in BTCUSDT/DOGEUSDT
 * fixtures ({@code ThresholdCalibrationTest}) — see docs/CHANGELOG.md's E8-F1-S1 entry for
 * the full candidate grid and expectancy numbers. That pass moved RSI from the original
 * hand-picked 30/70 to 25/75: widening the neutral band consistently raised win rate and
 * expectancy on the BUY side across both fixtures and every checkpoint, with a *larger*
 * sample at each candidate (fewer RSI-vs-other-indicator conflicts got resolved to HOLD),
 * so the gain isn't a small-n artifact. {@link #VOLATILITY_EXTREME_THRESHOLD} and
 * {@link #VOLUME_DRIED_UP_THRESHOLD} showed no comparable signal in the swept range and were
 * left unchanged — the only volatility candidate that looked better (5.0) did so on an n=10
 * sample, not enough to trust.
 *
 * <p><b>E8-F4-S1's out-of-sample validation found this shift does not replicate uniformly:</b>
 * the SELL-side widening holds on held-out BTCUSDT/DOGEUSDT tails and a genuinely untouched
 * SOLUSDT fixture, but the BUY-side widening — an equally central part of the original
 * finding above — reverses on two of those three out-of-sample checks, including SOLUSDT
 * (see {@code OutOfSampleValidationTest} and docs/CHANGELOG.md's E8-F4-S1 entry for the exact
 * figures). Per that story's confirmed report-only scope, RSI stays at 25/75/v2 here — the
 * BUY-side mismatch is a flagged finding for a future recalibration (e.g. asymmetric bounds),
 * not acted on yet. {@link #RULE_TABLE_VERSION} exists so revising these thresholds later is
 * an auditable, versioned change (feeds E6-F3-S2).
 */
public final class SignalRuleEngine {

    public static final String RULE_TABLE_VERSION = "v2";

    public static final BigDecimal RSI_OVERSOLD_THRESHOLD = new BigDecimal("25");
    public static final BigDecimal RSI_OVERBOUGHT_THRESHOLD = new BigDecimal("75");
    public static final BigDecimal VOLATILITY_EXTREME_THRESHOLD = new BigDecimal("8.0");
    public static final BigDecimal VOLUME_DRIED_UP_THRESHOLD = new BigDecimal("0.20");

    /**
     * The four calibration-candidate thresholds bundled together (E8-F1-S1), so
     * {@code ThresholdCalibrationTest} can sweep candidate values through {@link #evaluate}
     * without reflectively mutating these production {@code static final} constants.
     * {@link #DEFAULT} is what every production caller uses via the 5-arg {@link #evaluate}
     * overload.
     */
    public record RuleThresholds(BigDecimal rsiOversold, BigDecimal rsiOverbought, BigDecimal volatilityExtreme,
                                  BigDecimal volumeDriedUp) {

        public static final RuleThresholds DEFAULT = new RuleThresholds(RSI_OVERSOLD_THRESHOLD,
                RSI_OVERBOUGHT_THRESHOLD, VOLATILITY_EXTREME_THRESHOLD, VOLUME_DRIED_UP_THRESHOLD);
    }

    /**
     * The three indicators' bullish/bearish reads (E8-F3-S1), extracted out of {@link #evaluate}
     * so both this class's own unweighted vote-counting and {@link WeightedVoteRuleEngine}'s
     * weighted vote share the exact same "what counts as a bullish/bearish read" logic — one
     * source of truth, not two copies that could quietly drift apart. Public (not just
     * package-private) because {@code BacktestHarness} (a different package, under
     * {@code com.autotrade.dashboard.backtest}) also needs it to score each indicator's own
     * directional read independently, per that story's AC.
     */
    public record IndicatorVotes(boolean rsiBullish, boolean rsiBearish, boolean macdBullish, boolean macdBearish,
                                  boolean maBullish, boolean maBearish) {
    }

    private SignalRuleEngine() {
    }

    /** Computes each indicator's bullish/bearish read against {@code thresholds}, with no gating
     * or vote-counting applied — a pure per-indicator classification. See {@link IndicatorVotes}. */
    public static IndicatorVotes computeVotes(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                               RuleThresholds thresholds) {
        boolean rsiBullish = rsi.compareTo(thresholds.rsiOversold()) < 0;
        boolean rsiBearish = rsi.compareTo(thresholds.rsiOverbought()) > 0;
        boolean macdBullish = macd.histogram().signum() > 0;
        boolean macdBearish = macd.histogram().signum() < 0;
        boolean maBullish = movingAverage.relation() == MovingAverageRelation.SHORT_ABOVE_LONG;
        boolean maBearish = movingAverage.relation() == MovingAverageRelation.SHORT_BELOW_LONG;
        return new IndicatorVotes(rsiBullish, rsiBearish, macdBullish, macdBearish, maBullish, maBearish);
    }

    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend) {
        return evaluate(rsi, macd, movingAverage, volatility, volumeTrend, RuleThresholds.DEFAULT);
    }

    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend, RuleThresholds thresholds) {
        if (volumeTrend == null) {
            return SignalRuleId.NO_VOLUME_DATA;
        }
        if (volumeTrend.compareTo(thresholds.volumeDriedUp()) < 0) {
            return SignalRuleId.VOLUME_DRIED_UP;
        }
        if (volatility.compareTo(thresholds.volatilityExtreme()) > 0) {
            return SignalRuleId.VOLATILITY_TOO_EXTREME;
        }

        IndicatorVotes votes = computeVotes(rsi, macd, movingAverage, thresholds);

        int bullishCount = count(votes.rsiBullish(), votes.macdBullish(), votes.maBullish());
        int bearishCount = count(votes.rsiBearish(), votes.macdBearish(), votes.maBearish());

        if (bullishCount > 0 && bearishCount > 0) {
            return SignalRuleId.CONFLICTING_SIGNALS;
        }
        if (bullishCount == 3) {
            return SignalRuleId.BULLISH_UNANIMOUS;
        }
        if (bullishCount == 2) {
            return SignalRuleId.BULLISH_MAJORITY;
        }
        if (bearishCount == 3) {
            return SignalRuleId.BEARISH_UNANIMOUS;
        }
        if (bearishCount == 2) {
            return SignalRuleId.BEARISH_MAJORITY;
        }
        return SignalRuleId.NO_STRONG_SIGNAL;
    }

    private static int count(boolean a, boolean b, boolean c) {
        return (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
    }
}
