package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.indicator.AdxCalculator;
import com.autotrade.dashboard.indicator.IndicatorService;
import com.autotrade.dashboard.indicator.MacdCalculator;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageCrossoverCalculator;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.indicator.RsiCalculator;
import com.autotrade.dashboard.indicator.VolatilityCalculator;
import com.autotrade.dashboard.indicator.VolumeTrendCalculator;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.HoldTermCalculator;
import com.autotrade.dashboard.signal.IndicatorId;
import com.autotrade.dashboard.signal.Regime;
import com.autotrade.dashboard.signal.RegimeClassifier;
import com.autotrade.dashboard.signal.RegimeGatedRuleEngine;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleEngine.IndicatorVotes;
import com.autotrade.dashboard.signal.SignalRuleId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pure, in-memory walk-forward replay of the Buy/Sell/Hold rule table (E2-F4-S1) against a
 * real historical candle series — never touches MarketDataService/IndicatorService/
 * persistence, so a backtest run never writes a synthetic row into the real {@code signal_calls}
 * audit table E6-F3-S2 depends on for provenance.
 *
 * <p>Each decision point re-computes every indicator over {@code candles.subList(0, i + 1)} — a
 * growing window anchored at index 0 — exactly mirroring how {@code IndicatorService.compute()}
 * calls the same calculators in production (always the entire fetched candle list, never a
 * fixed trailing slice). Several calculators (MACD's EMA seed, RSI's Wilder seed) are anchored
 * at the list's start, so replaying with a fixed trailing window would produce different
 * numbers than production ever would have computed on that historical day.
 *
 * <p>E8-F5-S1: the TP/SL-aware walk-forward scoring primitives this class uses ({@link
 * WalkForwardScorer}, {@link DirectionalAccumulator}, {@link BacktestConfig}, {@link Checkpoint},
 * {@link CheckpointStats}, {@link DirectionalOutcome}, {@link DirectionalOutcomeStats}, {@link
 * DirectionalScoreResult}, {@link ExitReason}) were promoted out of this class into main scope so
 * {@code monitoring.LiveSignalDriftService} can reuse them against real forward market data —
 * this class (and {@code IndicatorAccumulator}/{@code HoldGateAccumulator}, which the live
 * monitor doesn't need) stays test-only.
 */
public final class BacktestHarness {

    private static final int MIN_CANDLES = IndicatorService.MIN_CANDLES_FOR_INDICATORS;

    private BacktestHarness() {
    }

    /** E8-F3-S1: a swappable combined-rule decision, matching {@link SignalRuleEngine#evaluate}'s
     * and {@code WeightedVoteRuleEngine.evaluate}'s shared 5-arg shape, so either can be replayed
     * through {@link #run(String, List, RuleEvaluator, SignalRuleEngine.RuleThresholds)}. */
    @FunctionalInterface
    public interface RuleEvaluator {
        SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                               BigDecimal volatility, BigDecimal volumeTrend);
    }

    public static BacktestReport run(String label, List<Candle> candles) {
        return run(label, candles, SignalRuleEngine.RuleThresholds.DEFAULT);
    }

    /** E8-F1-S1: accepts candidate {@link SignalRuleEngine.RuleThresholds} so
     * {@code ThresholdCalibrationTest} can sweep threshold candidates without touching
     * {@link SignalRuleEngine}'s production constants. Delegates to the {@link RuleEvaluator}
     * overload below, using {@link SignalRuleEngine#evaluate} itself (the unweighted table) as
     * the evaluator. */
    public static BacktestReport run(String label, List<Candle> candles, SignalRuleEngine.RuleThresholds thresholds) {
        return run(label, candles,
                (rsi, macd, ma, volatility, volumeTrend) -> SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend, thresholds),
                thresholds);
    }

    /**
     * E8-F3-S4: as {@link #run(String, List)}, but classifies regime against {@code
     * regimeThreshold} instead of the global default — the entry point {@code
     * PerSymbolAdxTrendingThresholdCalibrationTest} sweeps candidates through. Uses {@link
     * SignalRuleEngine.RuleThresholds#DEFAULT} for the underlying rule-table evaluation and leaves
     * the SELL regime gate unapplied, isolating the ADX axis from the already-shipped RSI/SELL-gate
     * axes so this sweep's BUY-side findings aren't confounded by either.
     */
    public static BacktestReport run(String label, List<Candle> candles, BigDecimal regimeThreshold) {
        SignalRuleEngine.RuleThresholds thresholds = SignalRuleEngine.RuleThresholds.DEFAULT;
        return run(label, candles,
                (rsi, macd, ma, volatility, volumeTrend) -> SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend, thresholds),
                thresholds, false, regimeThreshold);
    }

    /**
     * E8-F3-S1: accepts any {@link RuleEvaluator} (e.g. {@code WeightedVoteRuleEngine::evaluate})
     * in place of the hardcoded {@link SignalRuleEngine#evaluate} call, so a candidate rule
     * engine can be replayed through the exact same walk-forward machinery — decision points,
     * TP/SL-aware scoring, per-indicator scoring — as the production table, for a direct,
     * side-by-side "A/B against the current table" comparison ({@code
     * IndicatorExpectancyCalibrationTest}'s weighted-vs-unweighted run). {@code thresholds} is
     * threaded separately (not read off the evaluator) because per-indicator scoring
     * (E8-F3-S1) needs {@link SignalRuleEngine#computeVotes}'s own threshold-gated bullish/bearish
     * read regardless of which combined-rule evaluator is under test.
     */
    public static BacktestReport run(String label, List<Candle> candles, RuleEvaluator evaluator,
                                      SignalRuleEngine.RuleThresholds thresholds) {
        return run(label, candles, evaluator, thresholds, false);
    }

    /**
     * E8-F3-S3: as the 4-arg overload above, but when {@code applySellRegimeGate} is {@code true},
     * every decision point's {@code evaluator}-resolved rule additionally passes through {@link
     * RegimeGatedRuleEngine#applySellGate} using the regime this loop already computes for its
     * existing trending/ranging split reporting — the same wiring {@code SignalService} applies in
     * production, replayed here so {@code LiveDriftBaselineTest} can recompute {@code
     * LiveDriftBaseline}'s SELL constants against the gated behavior. Defaults to {@code false}
     * (today's ungated behavior) everywhere it isn't explicitly requested, so every existing caller
     * is unaffected.
     */
    public static BacktestReport run(String label, List<Candle> candles, RuleEvaluator evaluator,
                                      SignalRuleEngine.RuleThresholds thresholds, boolean applySellRegimeGate) {
        return run(label, candles, evaluator, thresholds, applySellRegimeGate, RegimeClassifier.ADX_TRENDING_THRESHOLD);
    }

    /**
     * E8-F3-S4: as the 5-arg overload above, but classifies every decision point's regime against
     * {@code regimeThreshold} instead of the hardcoded global {@link
     * RegimeClassifier#ADX_TRENDING_THRESHOLD} — lets {@code
     * PerSymbolAdxTrendingThresholdCalibrationTest} sweep candidate thresholds through the existing
     * {@code buyByRegime}/{@code sellByRegime} split without touching production code. One {@code
     * regime} value still feeds both the BUY and SELL regime-split accumulators for a given decision
     * point (this overload doesn't independently reclassify per direction), so a swept candidate's
     * {@code sellByRegime} numbers here are informational only — this story's ship bar is BUY-side
     * only, per its AC. Defaults to today's global threshold everywhere it isn't explicitly swept,
     * so every existing caller is unaffected.
     */
    public static BacktestReport run(String label, List<Candle> candles, RuleEvaluator evaluator,
                                      SignalRuleEngine.RuleThresholds thresholds, boolean applySellRegimeGate,
                                      BigDecimal regimeThreshold) {
        Map<SignalRuleId, Integer> callCounts = new EnumMap<>(SignalRuleId.class);
        Map<SignalRuleId, DirectionalAccumulator> directional = new EnumMap<>(SignalRuleId.class);
        Map<SignalRuleId, HoldGateAccumulator> holdGate = new EnumMap<>(SignalRuleId.class);
        Map<IndicatorId, IndicatorAccumulator> indicatorAcc = new EnumMap<>(IndicatorId.class);
        List<BacktestDecisionPoint> buySellPoints = new ArrayList<>();

        // E8-F3-S2: regime-split accumulators, additive alongside the existing per-rule ones —
        // every BUY/SELL decision point is tallied into exactly one of these four in addition to
        // its existing per-rule DirectionalAccumulator, so trending vs. ranging expectancy can be
        // compared directly for the same direction.
        DirectionalAccumulator buyTrendingAcc = new DirectionalAccumulator();
        DirectionalAccumulator buyRangingAcc = new DirectionalAccumulator();
        DirectionalAccumulator sellTrendingAcc = new DirectionalAccumulator();
        DirectionalAccumulator sellRangingAcc = new DirectionalAccumulator();

        for (SignalRuleId ruleId : SignalRuleId.values()) {
            callCounts.put(ruleId, 0);
            if (ruleId.call() == SignalCall.BUY || ruleId.call() == SignalCall.SELL) {
                directional.put(ruleId, new DirectionalAccumulator());
            } else {
                holdGate.put(ruleId, new HoldGateAccumulator());
            }
        }
        for (IndicatorId indicatorId : IndicatorId.values()) {
            indicatorAcc.put(indicatorId, new IndicatorAccumulator());
        }

        int decisionPoints = 0;
        for (int i = MIN_CANDLES - 1; i < candles.size(); i++) {
            List<Candle> window = candles.subList(0, i + 1);
            decisionPoints++;

            BigDecimal rsi = RsiCalculator.calculate(window, RsiCalculator.DEFAULT_PERIOD);
            MacdResult macd = MacdCalculator.calculate(window, MacdCalculator.DEFAULT_FAST_PERIOD,
                    MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD);
            MovingAverageResult ma = MovingAverageCrossoverCalculator.calculate(window,
                    MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);
            BigDecimal volatility = VolatilityCalculator.calculate(window, VolatilityCalculator.DEFAULT_PERIOD);
            BigDecimal volumeTrend = VolumeTrendCalculator.calculate(window,
                    VolumeTrendCalculator.DEFAULT_SHORT_PERIOD, VolumeTrendCalculator.DEFAULT_LONG_PERIOD);
            BigDecimal adx = AdxCalculator.calculate(window, AdxCalculator.DEFAULT_PERIOD);
            Regime regime = RegimeClassifier.classify(adx, regimeThreshold);

            SignalRuleId ruleTableMatch = evaluator.evaluate(rsi, macd, ma, volatility, volumeTrend);
            SignalRuleId matchedRule = applySellRegimeGate
                    ? RegimeGatedRuleEngine.applySellGate(ruleTableMatch, regime)
                    : ruleTableMatch;
            HoldTerm holdTerm = HoldTermCalculator.calculate(matchedRule, volatility);

            callCounts.merge(matchedRule, 1, Integer::sum);
            BigDecimal decisionClose = candles.get(i).close();
            List<Candle> forward = candles.subList(i + 1, candles.size());

            IndicatorVotes votes = SignalRuleEngine.computeVotes(rsi, macd, ma, thresholds);
            scoreIndicator(forward, decisionClose, votes.rsiBullish(), votes.rsiBearish(),
                    indicatorAcc.get(IndicatorId.RSI));
            scoreIndicator(forward, decisionClose, votes.macdBullish(), votes.macdBearish(),
                    indicatorAcc.get(IndicatorId.MACD));
            scoreIndicator(forward, decisionClose, votes.maBullish(), votes.maBearish(),
                    indicatorAcc.get(IndicatorId.MA_CROSSOVER));

            if (holdTerm != null) {
                DirectionalAccumulator acc = directional.get(matchedRule);
                acc.totalCalls++;
                boolean isBuy = matchedRule.call() == SignalCall.BUY;
                int midDays = (int) Math.round((holdTerm.minDays() + holdTerm.maxDays()) / 2.0);

                Optional<WalkForwardScorer.CrossingEvent> crossing =
                        WalkForwardScorer.findFirstCrossing(forward, holdTerm.maxDays(), decisionClose, isBuy);
                Optional<DirectionalScoreResult> minResult =
                        WalkForwardScorer.score(forward, holdTerm.minDays(), decisionClose, isBuy, crossing);
                Optional<DirectionalScoreResult> midResult =
                        WalkForwardScorer.score(forward, midDays, decisionClose, isBuy, crossing);
                Optional<DirectionalScoreResult> maxResult =
                        WalkForwardScorer.score(forward, holdTerm.maxDays(), decisionClose, isBuy, crossing);

                acc.record(Checkpoint.MIN, minResult);
                acc.record(Checkpoint.MID, midResult);
                acc.record(Checkpoint.MAX, maxResult);

                DirectionalAccumulator regimeAcc = isBuy
                        ? (regime == Regime.TRENDING ? buyTrendingAcc : buyRangingAcc)
                        : (regime == Regime.TRENDING ? sellTrendingAcc : sellRangingAcc);
                regimeAcc.totalCalls++;
                regimeAcc.record(Checkpoint.MIN, minResult);
                regimeAcc.record(Checkpoint.MID, midResult);
                regimeAcc.record(Checkpoint.MAX, maxResult);

                buySellPoints.add(new BacktestDecisionPoint(i, candles.get(i).timestamp(), rsi, macd.histogram(),
                        volatility, volumeTrend, matchedRule, holdTerm, minResult, midResult, maxResult, regime));
            } else {
                HoldGateAccumulator acc = holdGate.get(matchedRule);
                acc.totalCalls++;
                scoreHoldGate(candles, i, decisionClose).ifPresent(acc::record);
            }
        }

        Map<SignalRuleId, DirectionalOutcomeStats> directionalStats = new EnumMap<>(SignalRuleId.class);
        directional.forEach((ruleId, acc) -> directionalStats.put(ruleId, acc.toStats()));
        Map<SignalRuleId, HoldGateStats> holdGateStats = new EnumMap<>(SignalRuleId.class);
        holdGate.forEach((ruleId, acc) -> holdGateStats.put(ruleId, acc.toStats()));
        Map<IndicatorId, CheckpointStats> indicatorStats = new EnumMap<>(IndicatorId.class);
        indicatorAcc.forEach((indicatorId, acc) -> indicatorStats.put(indicatorId, acc.toStats()));

        DirectionalOutcomeStats overallBuy = combine(directionalStats.get(SignalRuleId.BULLISH_UNANIMOUS),
                directionalStats.get(SignalRuleId.BULLISH_MAJORITY));
        DirectionalOutcomeStats overallSell = combine(directionalStats.get(SignalRuleId.BEARISH_UNANIMOUS),
                directionalStats.get(SignalRuleId.BEARISH_MAJORITY));

        RegimeSplitStats buyByRegime = new RegimeSplitStats(buyTrendingAcc.toStats(), buyRangingAcc.toStats());
        RegimeSplitStats sellByRegime = new RegimeSplitStats(sellTrendingAcc.toStats(), sellRangingAcc.toStats());

        return new BacktestReport(label, candles.size(), decisionPoints, callCounts, directionalStats, overallBuy,
                overallSell, holdGateStats, buySellPoints, indicatorStats, buyByRegime, sellByRegime);
    }

    /**
     * Scores one indicator's own directional read at this decision point (E8-F3-S1), independent
     * of the combined rule table's matched rule/hold-term, using the shared {@link
     * WalkForwardScorer} — but bounded by the fixed {@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}
     * horizon rather than a rule-derived hold term, since a lone indicator's read has no
     * hold-term of its own to derive one from (the same horizon {@link #scoreHoldGate} already
     * uses for the same reason). A no-op when the indicator gave a neutral (non-directional) read.
     */
    private static void scoreIndicator(List<Candle> forward, BigDecimal decisionClose,
                                        boolean bullish, boolean bearish, IndicatorAccumulator acc) {
        scoreIndicator(forward, decisionClose, bullish, bearish, acc, BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS,
                BacktestConfig.TAKE_PROFIT_PCT, BacktestConfig.STOP_LOSS_PCT);
    }

    /**
     * As the 5-arg overload above, but takes an explicit horizon/TP-SL instead of always reading
     * {@link BacktestConfig}'s fixed diagnostic constants (E8-F3-S5) — lets {@link
     * #runIndicatorExpectancy} replay per-indicator scoring at a horizon other than the fixed
     * 5-day one every other caller of the 5-arg overload still uses.
     */
    private static void scoreIndicator(List<Candle> forward, BigDecimal decisionClose,
                                        boolean bullish, boolean bearish, IndicatorAccumulator acc,
                                        int horizonDays, BigDecimal takeProfitPct, BigDecimal stopLossPct) {
        if (!bullish && !bearish) {
            return;
        }
        acc.totalCalls++;
        Optional<WalkForwardScorer.CrossingEvent> crossing = WalkForwardScorer.findFirstCrossing(forward,
                horizonDays, decisionClose, bullish, takeProfitPct, stopLossPct);
        Optional<DirectionalScoreResult> result = WalkForwardScorer.score(forward, horizonDays, decisionClose, bullish, crossing);
        acc.record(result);
    }

    /**
     * E8-F3-S5: replays each indicator's own bullish/bearish read (same {@link
     * SignalRuleEngine#computeVotes} source of truth as {@link #run}'s per-indicator scoring) at an
     * explicit {@code horizonDays}/{@code takeProfitPct}/{@code stopLossPct} instead of the fixed
     * {@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}/{@link BacktestConfig#TAKE_PROFIT_PCT}/
     * {@link BacktestConfig#STOP_LOSS_PCT} {@link #run} always uses — lets {@code
     * IndicatorExpectancyAlternateHorizonCalibrationTest} check whether {@code
     * WeightedVoteRuleEngine.IndicatorWeights.DEFAULT}'s all-zero calibration is an artifact of
     * that one fixed short horizon, per that record's own Javadoc ("a future recalibration, e.g.
     * against a longer horizon, produces a positive weight"). Deliberately narrower than {@link
     * #run}: doesn't compute the combined rule table's matched-rule/call-count/hold-gate/regime
     * bookkeeping, since none of that is horizon-dependent for this method's purpose and
     * replicating it here would be unused work.
     */
    public static Map<IndicatorId, CheckpointStats> runIndicatorExpectancy(List<Candle> candles, int horizonDays,
                                                                            BigDecimal takeProfitPct, BigDecimal stopLossPct) {
        Map<IndicatorId, IndicatorAccumulator> indicatorAcc = new EnumMap<>(IndicatorId.class);
        for (IndicatorId indicatorId : IndicatorId.values()) {
            indicatorAcc.put(indicatorId, new IndicatorAccumulator());
        }

        for (int i = MIN_CANDLES - 1; i < candles.size(); i++) {
            List<Candle> window = candles.subList(0, i + 1);

            BigDecimal rsi = RsiCalculator.calculate(window, RsiCalculator.DEFAULT_PERIOD);
            MacdResult macd = MacdCalculator.calculate(window, MacdCalculator.DEFAULT_FAST_PERIOD,
                    MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD);
            MovingAverageResult ma = MovingAverageCrossoverCalculator.calculate(window,
                    MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);

            BigDecimal decisionClose = candles.get(i).close();
            List<Candle> forward = candles.subList(i + 1, candles.size());

            IndicatorVotes votes = SignalRuleEngine.computeVotes(rsi, macd, ma, SignalRuleEngine.RuleThresholds.DEFAULT);
            scoreIndicator(forward, decisionClose, votes.rsiBullish(), votes.rsiBearish(),
                    indicatorAcc.get(IndicatorId.RSI), horizonDays, takeProfitPct, stopLossPct);
            scoreIndicator(forward, decisionClose, votes.macdBullish(), votes.macdBearish(),
                    indicatorAcc.get(IndicatorId.MACD), horizonDays, takeProfitPct, stopLossPct);
            scoreIndicator(forward, decisionClose, votes.maBullish(), votes.maBearish(),
                    indicatorAcc.get(IndicatorId.MA_CROSSOVER), horizonDays, takeProfitPct, stopLossPct);
        }

        Map<IndicatorId, CheckpointStats> indicatorStats = new EnumMap<>(IndicatorId.class);
        indicatorAcc.forEach((indicatorId, acc) -> indicatorStats.put(indicatorId, acc.toStats()));
        return indicatorStats;
    }

    private static Optional<HoldGateOutcome> scoreHoldGate(List<Candle> candles, int decisionIndex, BigDecimal decisionClose) {
        int futureIndex = decisionIndex + BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS;
        if (futureIndex >= candles.size()) {
            return Optional.empty();
        }
        BigDecimal pctChange = WalkForwardScorer.percentChange(decisionClose, candles.get(futureIndex).close()).abs();
        return Optional.of(pctChange.compareTo(BacktestConfig.LARGE_MOVE_THRESHOLD_PCT) > 0
                ? HoldGateOutcome.LARGE_MOVE : HoldGateOutcome.STABLE);
    }

    private static DirectionalOutcomeStats combine(DirectionalOutcomeStats a, DirectionalOutcomeStats b) {
        return new DirectionalOutcomeStats(a.totalCalls() + b.totalCalls(), combineCheckpoint(a.min(), b.min()),
                combineCheckpoint(a.mid(), b.mid()), combineCheckpoint(a.max(), b.max()));
    }

    /** Package-private (not {@code private}) so {@code IndicatorExpectancyCalibrationTest} can
     * combine one indicator's per-fixture {@link CheckpointStats} (BTCUSDT + DOGEUSDT) into a
     * single call-count-weighted figure to compute {@code WeightedVoteRuleEngine.IndicatorWeights.DEFAULT}
     * from — the same call-count-weighted-average treatment {@link #combine} already gives
     * UNANIMOUS+MAJORITY, just reused across fixtures instead of across rules. */
    static CheckpointStats combineCheckpoint(CheckpointStats a, CheckpointStats b) {
        int win = a.win() + b.win();
        int loss = a.loss() + b.loss();
        int scored = win + loss + a.wash() + b.wash();
        double avgWin = win == 0 ? 0.0 : (a.avgWinReturnPct() * a.win() + b.avgWinReturnPct() * b.win()) / win;
        double avgLoss = loss == 0 ? 0.0 : (a.avgLossReturnPct() * a.loss() + b.avgLossReturnPct() * b.loss()) / loss;
        double avgHoldingDays = scored == 0 ? 0.0
                : (a.avgHoldingDays() * a.scored() + b.avgHoldingDays() * b.scored()) / scored;
        return new CheckpointStats(win, loss, a.wash() + b.wash(), a.notScored() + b.notScored(), avgWin, avgLoss,
                a.tpHit() + b.tpHit(), a.slHit() + b.slHit(), a.horizonExpired() + b.horizonExpired(), avgHoldingDays);
    }

    /**
     * One {@link com.autotrade.dashboard.signal.IndicatorId}'s own directional-read outcome
     * tally (E8-F3-S1) — structurally {@link DirectionalAccumulator}'s single-checkpoint sibling:
     * a lone indicator has no rule-derived hold term to bracket a MIN/MID/MAX range with, so
     * there's exactly one horizon ({@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}) instead of
     * three. Reuses the existing {@link CheckpointStats} output shape unchanged. Test-only — the
     * live monitor (E8-F5-S1) doesn't need per-indicator scoring, only the combined rule table's
     * own BUY/SELL call.
     */
    private static final class IndicatorAccumulator {
        int totalCalls;
        final EnumMap<DirectionalOutcome, Integer> outcomeCounts = new EnumMap<>(DirectionalOutcome.class);
        final EnumMap<ExitReason, Integer> exitReasonCounts = new EnumMap<>(ExitReason.class);
        int notScored;
        BigDecimal winReturnSum = BigDecimal.ZERO;
        BigDecimal lossReturnSum = BigDecimal.ZERO;
        long holdingDaysSum;

        IndicatorAccumulator() {
            for (DirectionalOutcome outcome : DirectionalOutcome.values()) {
                outcomeCounts.put(outcome, 0);
            }
            for (ExitReason reason : ExitReason.values()) {
                exitReasonCounts.put(reason, 0);
            }
        }

        void record(Optional<DirectionalScoreResult> result) {
            if (result.isEmpty()) {
                notScored++;
                return;
            }
            DirectionalScoreResult scoreResult = result.get();
            outcomeCounts.merge(scoreResult.outcome(), 1, Integer::sum);
            exitReasonCounts.merge(scoreResult.exitReason(), 1, Integer::sum);
            holdingDaysSum += scoreResult.daysHeld();
            if (scoreResult.outcome() == DirectionalOutcome.WIN) {
                winReturnSum = winReturnSum.add(scoreResult.signedReturnPct());
            } else if (scoreResult.outcome() == DirectionalOutcome.LOSS) {
                lossReturnSum = lossReturnSum.add(scoreResult.signedReturnPct());
            }
        }

        CheckpointStats toStats() {
            int win = outcomeCounts.get(DirectionalOutcome.WIN);
            int loss = outcomeCounts.get(DirectionalOutcome.LOSS);
            int wash = outcomeCounts.get(DirectionalOutcome.WASH);
            int scored = win + loss + wash;
            double avgWin = win == 0 ? 0.0 : winReturnSum.doubleValue() / win;
            double avgLoss = loss == 0 ? 0.0 : lossReturnSum.doubleValue() / loss;
            double avgHoldingDays = scored == 0 ? 0.0 : holdingDaysSum / (double) scored;
            return new CheckpointStats(win, loss, wash, notScored, avgWin,
                    avgLoss, exitReasonCounts.get(ExitReason.TP_HIT), exitReasonCounts.get(ExitReason.SL_HIT),
                    exitReasonCounts.get(ExitReason.HORIZON_EXPIRED), avgHoldingDays);
        }
    }

    private static final class HoldGateAccumulator {
        int totalCalls;
        final EnumMap<HoldGateOutcome, Integer> counts = new EnumMap<>(HoldGateOutcome.class);

        HoldGateAccumulator() {
            for (HoldGateOutcome outcome : HoldGateOutcome.values()) {
                counts.put(outcome, 0);
            }
        }

        void record(HoldGateOutcome outcome) {
            counts.merge(outcome, 1, Integer::sum);
        }

        HoldGateStats toStats() {
            int scoredCount = counts.get(HoldGateOutcome.LARGE_MOVE) + counts.get(HoldGateOutcome.STABLE);
            return new HoldGateStats(totalCalls, scoredCount, counts.get(HoldGateOutcome.LARGE_MOVE));
        }
    }
}
