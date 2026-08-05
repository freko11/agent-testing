package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.signal.SignalRuleEngine.IndicatorVotes;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;

import java.math.BigDecimal;

/**
 * E8-F3-S1: an alternative to {@link SignalRuleEngine}'s unweighted 2-of-3 vote — each
 * indicator's bullish/bearish read is weighted by its own backtested expectancy, so the
 * strongest-performing indicator gets proportionally more influence on the call, rather than
 * every indicator counting the same regardless of how well it actually predicts.
 *
 * <p><b>Deliberately not wired into production.</b> {@code SignalService}/{@code OrderService}
 * still call {@link SignalRuleEngine#evaluate} exactly as before this story — this class exists
 * standalone so it can be evaluated/backtested side by side with the current table ({@link
 * #evaluateUnweighted}), the literal "fallback/comparison mode" this story's AC asks for. Wiring
 * this in (a config flag, an {@code OrderService} change, a {@code RULE_TABLE_VERSION} bump) is
 * explicitly out of scope here, pending E8-F4-S1's out-of-sample validation — {@link
 * IndicatorWeights#DEFAULT} below is tuned and evaluated on the same two checked-in
 * BTCUSDT/DOGEUSDT fixtures, the same overfitting caveat {@code ThresholdCalibrationTest}
 * (E8-F1-S1) already documents for threshold calibration.
 *
 * <p>Reuses {@link SignalRuleEngine#computeVotes} for "what counts as a bullish/bearish read" (one
 * source of truth, not a second copy) and keeps its three safety gates and its conflict/dissent
 * gate byte-for-byte unchanged — weighting never overrides "at least one indicator disagrees",
 * that stays a hard HOLD regardless of how confident the majority is.
 */
public final class WeightedVoteRuleEngine {

    /**
     * Per-indicator weight = {@code max(0, expectancyPctAfterCosts)} from {@code
     * BacktestHarness}'s new per-indicator scoring (E8-F3-S1), combined call-count-weighted
     * across the checked-in BTCUSDT and DOGEUSDT fixtures (the same combination {@code
     * BacktestHarness.combine} already uses for UNANIMOUS+MAJORITY roll-ups, applied here across
     * fixtures instead of across rules). A negative-expectancy indicator floors to zero weight —
     * silenced, but never made to vote against its own direction — rather than going negative,
     * so a consistently-wrong indicator can only lose influence, never actively invert the vote.
     *
     * <p><b>Computed finding (E8-F3-S1):</b> under the fixed 5-day {@code
     * HOLD_REFERENCE_HORIZON_DAYS}/5%-TP/3%-SL scoring this weight is derived from, all three
     * indicators' combined after-cost expectancy came back negative on the two checked-in
     * fixtures (RSI -0.728%, MACD -0.039%, MA-crossover -0.131% — stop-loss hits substantially
     * outnumber take-profit hits for every indicator at this short a horizon), so every weight
     * here floors to zero. This is a real, computed result, not a placeholder: with {@code
     * DEFAULT}, {@link #evaluate} can only ever resolve UNANIMOUS (still reachable unconditionally
     * off the raw 3-of-3 vote count) or NO_STRONG_SIGNAL — the "lone dominant indicator" and
     * "2-of-3 majority" promotion paths this class adds are real and independently proven by
     * {@code WeightedVoteRuleEngineTest} using non-default weights, but are dormant under this
     * specific calibration until a future recalibration (e.g. against a longer horizon, or after
     * E8-F4-S1's out-of-sample pass) produces a positive weight for at least one indicator. See
     * {@code IndicatorExpectancyCalibrationTest} for the run that produced these numbers and
     * docs/CHANGELOG.md's E8-F3-S1 entry for the printed per-indicator report they were read from.
     */
    public record IndicatorWeights(BigDecimal rsiWeight, BigDecimal macdWeight, BigDecimal maCrossoverWeight) {

        public static final IndicatorWeights DEFAULT = new IndicatorWeights(
                new BigDecimal("0.000"), new BigDecimal("0.000"), new BigDecimal("0.000"));
    }

    /**
     * Fraction of {@code totalWeight} a lone or 2-of-3 weighted vote must clear to resolve
     * BULLISH_MAJORITY/BEARISH_MAJORITY rather than NO_STRONG_SIGNAL. An uncalibrated placeholder
     * (a simple "more than half the total weighted confidence" bar), the same treatment as {@code
     * BacktestConfig.TAKE_PROFIT_PCT}/{@code STOP_LOSS_PCT} — chosen for being a defensible,
     * easy-to-reason-about default (majority of weight, mirroring "majority of votes" in the
     * unweighted table), not backtest-derived. A future story could sweep this the same way
     * E8-F1-S1 swept RSI thresholds.
     */
    public static final BigDecimal WEIGHTED_MAJORITY_FRACTION = new BigDecimal("0.5");

    private WeightedVoteRuleEngine() {
    }

    /** Weighted vote using {@link IndicatorWeights#DEFAULT}. See the 7-arg overload. */
    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend) {
        return evaluate(rsi, macd, movingAverage, volatility, volumeTrend, RuleThresholds.DEFAULT,
                IndicatorWeights.DEFAULT);
    }

    /**
     * Weighted vote: same three safety gates and the same conflict/dissent gate as {@link
     * SignalRuleEngine#evaluate}, but once those have passed, BULLISH/BEARISH UNANIMOUS/MAJORITY
     * is decided by summed indicator weight rather than a raw 2-of-3 count.
     *
     * <p>All-three-agree always resolves UNANIMOUS regardless of {@code weights} — checked via
     * the raw vote count ({@code bullishCount == 3}), not a {@code weightedBullish >= totalWeight}
     * comparison, specifically so a single zero-weighted (silenced) indicator agreeing alongside
     * the other two can never accidentally read as "only 2 of 3 actually voted" reaching the
     * UNANIMOUS bar on weight alone. A single dominant indicator (or 2-of-3) can newly resolve
     * BULLISH_MAJORITY/BEARISH_MAJORITY where the unweighted table would have called
     * NO_STRONG_SIGNAL — that's the intended "proportionally more influence" behavior this story
     * exists to add, not an edge case to guard against.
     *
     * <p>When {@code totalWeight} is zero (every indicator's weight floored to zero, {@link
     * IndicatorWeights#DEFAULT}'s own current calibration — see its Javadoc), a 1-or-2-vote
     * weighted sum is also zero and a naive {@code weightedSum >= totalWeight * fraction}
     * comparison would vacuously read {@code 0 >= 0} as true, "promoting" every lone/majority
     * vote regardless of which indicator it came from. Guarded explicitly: with nothing to weigh,
     * only the raw-count UNANIMOUS branch can still resolve a call.
     */
    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend, RuleThresholds thresholds,
                                         IndicatorWeights weights) {
        if (volumeTrend == null) {
            return SignalRuleId.NO_VOLUME_DATA;
        }
        if (volumeTrend.compareTo(thresholds.volumeDriedUp()) < 0) {
            return SignalRuleId.VOLUME_DRIED_UP;
        }
        if (volatility.compareTo(thresholds.volatilityExtreme()) > 0) {
            return SignalRuleId.VOLATILITY_TOO_EXTREME;
        }

        IndicatorVotes votes = SignalRuleEngine.computeVotes(rsi, macd, movingAverage, thresholds);
        int bullishCount = count(votes.rsiBullish(), votes.macdBullish(), votes.maBullish());
        int bearishCount = count(votes.rsiBearish(), votes.macdBearish(), votes.maBearish());

        if (bullishCount > 0 && bearishCount > 0) {
            return SignalRuleId.CONFLICTING_SIGNALS;
        }

        BigDecimal totalWeight = weights.rsiWeight().add(weights.macdWeight()).add(weights.maCrossoverWeight());
        BigDecimal majorityThreshold = totalWeight.multiply(WEIGHTED_MAJORITY_FRACTION);

        if (bullishCount == 3) {
            return SignalRuleId.BULLISH_UNANIMOUS;
        }
        if (bullishCount > 0) {
            return clearsMajorityBar(votes.rsiBullish(), votes.macdBullish(), votes.maBullish(), weights, totalWeight, majorityThreshold)
                    ? SignalRuleId.BULLISH_MAJORITY : SignalRuleId.NO_STRONG_SIGNAL;
        }
        if (bearishCount == 3) {
            return SignalRuleId.BEARISH_UNANIMOUS;
        }
        if (bearishCount > 0) {
            return clearsMajorityBar(votes.rsiBearish(), votes.macdBearish(), votes.maBearish(), weights, totalWeight, majorityThreshold)
                    ? SignalRuleId.BEARISH_MAJORITY : SignalRuleId.NO_STRONG_SIGNAL;
        }
        return SignalRuleId.NO_STRONG_SIGNAL;
    }

    /** {@code false} when {@code totalWeight} is zero (see {@link #evaluate}'s Javadoc on the
     * vacuous-comparison guard this avoids) or the weighted sum of the indicators that actually
     * voted falls short of {@code majorityThreshold}. */
    private static boolean clearsMajorityBar(boolean rsiVoted, boolean macdVoted, boolean maVoted,
                                              IndicatorWeights weights, BigDecimal totalWeight, BigDecimal majorityThreshold) {
        if (totalWeight.signum() <= 0) {
            return false;
        }
        return weightedSum(rsiVoted, macdVoted, maVoted, weights).compareTo(majorityThreshold) >= 0;
    }

    /**
     * The unweighted fallback/comparison mode this story's AC asks for: a pure delegation to
     * {@link SignalRuleEngine#evaluate}, so both modes are directly callable side by side against
     * the same inputs (see {@code WeightedVoteRuleEngineTest}/{@code IndicatorExpectancyCalibrationTest}).
     */
    public static SignalRuleId evaluateUnweighted(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                                    BigDecimal volatility, BigDecimal volumeTrend) {
        return SignalRuleEngine.evaluate(rsi, macd, movingAverage, volatility, volumeTrend);
    }

    private static BigDecimal weightedSum(boolean rsiVoted, boolean macdVoted, boolean maVoted, IndicatorWeights weights) {
        BigDecimal sum = BigDecimal.ZERO;
        if (rsiVoted) {
            sum = sum.add(weights.rsiWeight());
        }
        if (macdVoted) {
            sum = sum.add(weights.macdWeight());
        }
        if (maVoted) {
            sum = sum.add(weights.maCrossoverWeight());
        }
        return sum;
    }

    private static int count(boolean a, boolean b, boolean c) {
        return (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
    }
}
