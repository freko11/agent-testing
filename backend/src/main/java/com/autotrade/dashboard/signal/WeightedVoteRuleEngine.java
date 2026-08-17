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
 * explicitly out of scope here. {@link IndicatorWeights#DEFAULT} below was tuned on the same two
 * checked-in BTCUSDT/DOGEUSDT fixtures, the same overfitting caveat {@code ThresholdCalibrationTest}
 * (E8-F1-S1) already documents for threshold calibration. E8-F4-S1's out-of-sample validation
 * confirmed the original all-zero calibration (fixed 5-day horizon) replicated on held-out data;
 * E8-F3-S5 then re-attempted the calibration at longer horizons and found MACD's weight does clear
 * the bar there — see {@link IndicatorWeights#DEFAULT}'s own Javadoc for the full account of both
 * passes and docs/CHANGELOG.md's E8-F3-S1/E8-F4-S1/E8-F3-S5 entries for the printed reports.
 * E8-F3-S6 then swept {@link #WEIGHTED_MAJORITY_FRACTION} — the one calibratable constant in this
 * class {@code IndicatorWeights.DEFAULT} left untried, now that E8-F3-S5 gave it a real nonzero
 * weight to act on — and found no candidate beats the shipped 0.5; see that constant's own Javadoc
 * for the full three-regime breakdown.
 *
 * <p>Reuses {@link SignalRuleEngine#computeVotes} for "what counts as a bullish/bearish read" (one
 * source of truth, not a second copy) and keeps its three safety gates and its conflict/dissent
 * gate byte-for-byte unchanged — weighting never overrides "at least one indicator disagrees",
 * that stays a hard HOLD regardless of how confident the majority is.
 */
public final class WeightedVoteRuleEngine {

    /**
     * Per-indicator weight = {@code max(0, expectancyPctAfterCosts)} from {@code
     * BacktestHarness}'s per-indicator scoring, combined call-count-weighted across the checked-in
     * BTCUSDT and DOGEUSDT fixtures (the same combination {@code BacktestHarness.combine} already
     * uses for UNANIMOUS+MAJORITY roll-ups, applied here across fixtures instead of across rules).
     * A negative-expectancy indicator floors to zero weight — silenced, but never made to vote
     * against its own direction — rather than going negative, so a consistently-wrong indicator
     * can only lose influence, never actively invert the vote.
     *
     * <p><b>E8-F3-S1's original finding</b> — under the fixed 5-day {@code
     * HOLD_REFERENCE_HORIZON_DAYS}/5%-TP/3%-SL scoring — was all three indicators' combined
     * after-cost expectancy coming back negative (RSI -0.728%, MACD -0.039%, MA-crossover -0.131%
     * — stop-loss hits substantially outnumbered take-profit hits for every indicator at this
     * short a horizon), an all-zero result E8-F4-S1's out-of-sample pass ({@code
     * OutOfSampleValidationTest}) confirmed replicates on held-out and genuinely untouched data.
     * That Javadoc named "a future recalibration (e.g. against a longer horizon)" as the one
     * untried lever — E8-F3-S5 tried it.
     *
     * <p><b>E8-F3-S5's re-attempt:</b> {@code IndicatorExpectancyAlternateHorizonCalibrationTest}
     * re-ran the same tuning fixtures at two longer, TP/SL-proportionally-scaled horizons: 10 days
     * (TP10%/SL6%) and 15 days (TP15%/SL9%, anchored to {@code HoldTermRule.STRONG_LOW}'s own
     * {@code maxDays} rather than picked arbitrarily). MACD's combined after-cost expectancy came
     * back positive at both (+0.289% at 10 days, +0.714% at 15 days); MA-crossover only at 15 days
     * (+0.162%); RSI stayed negative at every horizon tested, so it was never a shipping candidate
     * here. Per this story's confirmed ship bar, the 15-day candidate's positive results were
     * checked against the same held-out BTCUSDT/DOGEUSDT tails plus the untouched SOLUSDT fixture
     * E8-F4-S1 used, at that same 15-day/TP15%/SL9% horizon:
     * <ul>
     *   <li><b>MACD held up cleanly</b> — combined held-out after-cost expectancy +0.845%, and
     *   positive on <i>all three</i> individual surfaces independently (BTCUSDT +1.930%, DOGEUSDT
     *   +0.132%, SOLUSDT +0.746%), not just in aggregate. Shipped: {@code macdWeight} 0.000 →
     *   0.714 (the tuning-set combined figure, same "ship the tuning-set value, confirm it holds
     *   out-of-sample" methodology E8-F3-S1/E8-F4-S1 established).</li>
     *   <li><b>MA-crossover did not hold up</b> — its barely-positive combined held-out figure
     *   (+0.038%) is carried entirely by DOGEUSDT (+1.533%); BTCUSDT (-0.274%) and SOLUSDT
     *   (-0.275%) are both negative. The same "one fixture masks a two-of-three-disagreeing
     *   result" pattern every other E8-F1 per-symbol axis (RSI, MACD-histogram-magnitude,
     *   MA-crossover-separation) already found doesn't generalize. Stays at 0.000, no ship.</li>
     * </ul>
     * With {@code DEFAULT}, {@link #evaluate}'s totalWeight is now 0.714 (entirely from MACD): a
     * lone-or-majority vote that includes a bullish/bearish MACD read always clears the weighted-
     * majority bar (MACD's own weight already equals all of a nonzero totalWeight, so {@code
     * weightedSum >= totalWeight * WEIGHTED_MAJORITY_FRACTION} holds trivially whenever MACD
     * voted), newly resolving BULLISH_MAJORITY/BEARISH_MAJORITY where the unweighted table would
     * call NO_STRONG_SIGNAL — the intended "proportionally more influence" behavior this story
     * exists to add. A lone RSI-only or MA-only vote (MACD neutral) still resolves NO_STRONG_SIGNAL
     * (their own weight is still 0). UNANIMOUS is unaffected either way (decided off the raw 3-of-3
     * count, not weight). See {@code IndicatorExpectancyAlternateHorizonCalibrationTest} for the
     * run that produced these numbers and docs/CHANGELOG.md's E8-F3-S5 entry for the full printed
     * report. Not re-validated at a third horizon — a future recalibration story, same open-ended
     * caveat this Javadoc already carried before E8-F3-S5 closed the first one out.
     */
    public record IndicatorWeights(BigDecimal rsiWeight, BigDecimal macdWeight, BigDecimal maCrossoverWeight) {

        public static final IndicatorWeights DEFAULT = new IndicatorWeights(
                new BigDecimal("0.000"), new BigDecimal("0.714"), new BigDecimal("0.000"));
    }

    /**
     * Fraction of {@code totalWeight} a lone or 2-of-3 weighted vote must clear to resolve
     * BULLISH_MAJORITY/BEARISH_MAJORITY rather than NO_STRONG_SIGNAL. An uncalibrated placeholder
     * (a simple "more than half the total weighted confidence" bar), the same treatment as {@code
     * BacktestConfig.TAKE_PROFIT_PCT}/{@code STOP_LOSS_PCT} — chosen for being a defensible,
     * easy-to-reason-about default (majority of weight, mirroring "majority of votes" in the
     * unweighted table), not backtest-derived.
     *
     * <p><b>E8-F3-S6 swept this constant, now that {@link IndicatorWeights#DEFAULT} carries a real
     * nonzero weight (E8-F3-S5's {@code macdWeight = 0.714}) and this constant is no longer
     * structurally inert.</b> Worked out directly from {@link #evaluate(BigDecimal, MacdResult,
     * MovingAverageResult, BigDecimal, BigDecimal, RuleThresholds, IndicatorWeights, BigDecimal)}'s
     * own code before running anything: with {@code DEFAULT}'s weights, {@code totalWeight} is
     * always exactly 0.714 (entirely from MACD), and a lone-or-2-of-3 vote's weighted sum can only
     * ever be 0.714 (MACD voted in that direction) or 0.000 (it didn't) — so the entire real-valued
     * fraction range collapses to exactly three behavioral regimes, not a continuum: {@code
     * fraction == 0} (most permissive — every lone/2-of-3 vote promotes, including ones driven
     * entirely by still-zero-weighted RSI/MA-crossover), {@code 0 < fraction <= 1} (only
     * MACD-inclusive votes promote — every value here, including the shipped 0.5, is provably
     * byte-identical), and {@code fraction > 1} (least permissive — no lone/2-of-3 vote can ever
     * promote, only the raw-count UNANIMOUS branch still resolves a call).
     *
     * <p>{@code WeightedMajorityFractionCalibrationTest} swept one representative candidate per
     * regime (0.00, 0.50, 1.50) against the combined BTCUSDT+DOGEUSDT tuning fixtures at the same
     * 15-day/TP15%/SL9% horizon {@code macdWeight} was itself calibrated at (E8-F3-S5), via a new
     * {@code BacktestHarness.runCombinedCallExpectancy} (the combined rule-table call's own
     * expectancy, not a single indicator's). Result, empirically confirmed rather than only
     * theoretically derived: {@code fraction = 0.00} produced a byte-identical report to {@code
     * 0.50} on both fixtures (combined after-cost expectancy +0.921%, n=825/838) — not because the
     * math forces it in general, but because in this real data MACD's histogram is essentially
     * never exactly zero, so every lone/2-of-3 vote that exists already includes MACD (confirmed
     * directly: zero RSI-only or MA-only lone/2-of-3 votes found across both fixtures' combined
     * ~1900 decision points). {@code fraction = 1.50} produced zero scored calls on both fixtures —
     * BULLISH_UNANIMOUS/BEARISH_UNANIMOUS never fire in this data at all (the same finding {@code
     * WeightedVoteBacktestTest}/E8-F3-S1 already documented), so disabling majority resolution
     * entirely disables the engine's output here. Net: no candidate clears "beats the current
     * default" — 0.00 ties, 1.50 (and everything above 1) is strictly worse (an unusable,
     * always-empty call population) — so this stays at 0.5, unchanged. See {@code
     * WeightedMajorityFractionCalibrationTest}'s class Javadoc for the full regime breakdown and
     * docs/CHANGELOG.md's E8-F3-S6 entry for the printed report.
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
        return evaluate(rsi, macd, movingAverage, volatility, volumeTrend, thresholds, weights, WEIGHTED_MAJORITY_FRACTION);
    }

    /**
     * E8-F3-S6: as the 7-arg overload above, but takes an explicit {@code majorityFraction} instead
     * of always reading the static {@link #WEIGHTED_MAJORITY_FRACTION} — the same calibration seam
     * {@code RuleThresholds}-accepting overloads (E8-F1-S1) and {@code BacktestHarness}'s {@code
     * regimeThreshold} overload (E8-F3-S4) already established for their own tunable constants, so
     * {@code WeightedMajorityFractionCalibrationTest} can sweep candidate fractions without mutating
     * the production constant. The 7-arg overload delegates here with {@code WEIGHTED_MAJORITY_FRACTION}
     * so every existing caller is unaffected.
     */
    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend, RuleThresholds thresholds,
                                         IndicatorWeights weights, BigDecimal majorityFraction) {
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
        BigDecimal majorityThreshold = totalWeight.multiply(majorityFraction);

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
