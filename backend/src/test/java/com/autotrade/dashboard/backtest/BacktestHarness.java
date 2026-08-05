package com.autotrade.dashboard.backtest;

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
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 */
public final class BacktestHarness {

    private static final int MIN_CANDLES = IndicatorService.MIN_CANDLES_FOR_INDICATORS;

    private BacktestHarness() {
    }

    public static BacktestReport run(String label, List<Candle> candles) {
        return run(label, candles, SignalRuleEngine.RuleThresholds.DEFAULT);
    }

    /** E8-F1-S1: accepts candidate {@link SignalRuleEngine.RuleThresholds} so
     * {@code ThresholdCalibrationTest} can sweep threshold candidates without touching
     * {@link SignalRuleEngine}'s production constants. */
    public static BacktestReport run(String label, List<Candle> candles, SignalRuleEngine.RuleThresholds thresholds) {
        Map<SignalRuleId, Integer> callCounts = new EnumMap<>(SignalRuleId.class);
        Map<SignalRuleId, DirectionalAccumulator> directional = new EnumMap<>(SignalRuleId.class);
        Map<SignalRuleId, HoldGateAccumulator> holdGate = new EnumMap<>(SignalRuleId.class);
        List<BacktestDecisionPoint> buySellPoints = new ArrayList<>();

        for (SignalRuleId ruleId : SignalRuleId.values()) {
            callCounts.put(ruleId, 0);
            if (ruleId.call() == SignalCall.BUY || ruleId.call() == SignalCall.SELL) {
                directional.put(ruleId, new DirectionalAccumulator());
            } else {
                holdGate.put(ruleId, new HoldGateAccumulator());
            }
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

            SignalRuleId matchedRule = SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend, thresholds);
            HoldTerm holdTerm = HoldTermCalculator.calculate(matchedRule, volatility);

            callCounts.merge(matchedRule, 1, Integer::sum);
            BigDecimal decisionClose = candles.get(i).close();

            if (holdTerm != null) {
                DirectionalAccumulator acc = directional.get(matchedRule);
                acc.totalCalls++;
                boolean isBuy = matchedRule.call() == SignalCall.BUY;
                int midDays = (int) Math.round((holdTerm.minDays() + holdTerm.maxDays()) / 2.0);

                Optional<CrossingEvent> crossing = findFirstCrossing(candles, i, holdTerm.maxDays(), decisionClose, isBuy);
                Optional<DirectionalScoreResult> minResult = score(candles, i, holdTerm.minDays(), decisionClose, isBuy, crossing);
                Optional<DirectionalScoreResult> midResult = score(candles, i, midDays, decisionClose, isBuy, crossing);
                Optional<DirectionalScoreResult> maxResult = score(candles, i, holdTerm.maxDays(), decisionClose, isBuy, crossing);

                acc.record(Checkpoint.MIN, minResult);
                acc.record(Checkpoint.MID, midResult);
                acc.record(Checkpoint.MAX, maxResult);

                buySellPoints.add(new BacktestDecisionPoint(i, candles.get(i).timestamp(), rsi, macd.histogram(),
                        volatility, volumeTrend, matchedRule, holdTerm, minResult, midResult, maxResult));
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

        DirectionalOutcomeStats overallBuy = combine(directionalStats.get(SignalRuleId.BULLISH_UNANIMOUS),
                directionalStats.get(SignalRuleId.BULLISH_MAJORITY));
        DirectionalOutcomeStats overallSell = combine(directionalStats.get(SignalRuleId.BEARISH_UNANIMOUS),
                directionalStats.get(SignalRuleId.BEARISH_MAJORITY));

        return new BacktestReport(label, candles.size(), decisionPoints, callCounts, directionalStats, overallBuy,
                overallSell, holdGateStats, buySellPoints);
    }

    /**
     * @param crossing this decision point's shared TP/SL scan result (E8-F2-S1), if any — applied
     *                  to this checkpoint only when it occurred at or before {@code daysForward},
     *                  so an early crossing doesn't collapse MIN/MID/MAX to identical results.
     * @return empty if {@code daysForward} candles past the decision point don't exist in the
     *         fixture and no TP/SL crossing within {@code daysForward} resolved it first.
     */
    static Optional<DirectionalScoreResult> score(List<Candle> candles, int decisionIndex, int daysForward,
                                                    BigDecimal decisionClose, boolean isBuy,
                                                    Optional<CrossingEvent> crossing) {
        if (crossing.isPresent() && crossing.get().daysForward() <= daysForward) {
            CrossingEvent event = crossing.get();
            DirectionalOutcome outcome = event.exitReason() == ExitReason.TP_HIT ? DirectionalOutcome.WIN : DirectionalOutcome.LOSS;
            return Optional.of(new DirectionalScoreResult(outcome, event.signedReturnPct(), event.exitReason()));
        }

        int futureIndex = decisionIndex + daysForward;
        if (futureIndex >= candles.size()) {
            return Optional.empty();
        }
        BigDecimal pctChange = percentChange(decisionClose, candles.get(futureIndex).close());
        BigDecimal signedForCall = isBuy ? pctChange : pctChange.negate();

        DirectionalOutcome outcome = signedForCall.abs().compareTo(BacktestConfig.WIN_LOSS_DEADBAND_PCT) <= 0
                ? DirectionalOutcome.WASH
                : (signedForCall.signum() > 0 ? DirectionalOutcome.WIN : DirectionalOutcome.LOSS);
        return Optional.of(new DirectionalScoreResult(outcome, signedForCall, ExitReason.HORIZON_EXPIRED));
    }

    /**
     * Day-by-day walk-forward scan (E8-F2-S1) for the first day, within {@code maxDaysForward} of
     * the decision point, whose high/low crosses the take-profit or stop-loss price implied by
     * {@link BacktestConfig#TAKE_PROFIT_PCT}/{@link BacktestConfig#STOP_LOSS_PCT}. Runs once per
     * decision point and is then applied to each {@link Checkpoint} independently by {@link
     * #score}, bounded by that checkpoint's own day count.
     *
     * <p>A single daily OHLC bar can't say whether the high or low happened first intraday — if
     * both TP and SL cross on the same day, stop-loss wins (the conservative assumption).
     */
    static Optional<CrossingEvent> findFirstCrossing(List<Candle> candles, int decisionIndex,
                                                       int maxDaysForward, BigDecimal decisionClose,
                                                       boolean isBuy) {
        BigDecimal tpDistance = decisionClose.multiply(BacktestConfig.TAKE_PROFIT_PCT)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal slDistance = decisionClose.multiply(BacktestConfig.STOP_LOSS_PCT)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal tpPrice = isBuy ? decisionClose.add(tpDistance) : decisionClose.subtract(tpDistance);
        BigDecimal slPrice = isBuy ? decisionClose.subtract(slDistance) : decisionClose.add(slDistance);

        int lastIndex = Math.min(decisionIndex + maxDaysForward, candles.size() - 1);
        for (int i = decisionIndex + 1; i <= lastIndex; i++) {
            Candle candle = candles.get(i);
            boolean slHit = isBuy ? candle.low().compareTo(slPrice) <= 0 : candle.high().compareTo(slPrice) >= 0;
            if (slHit) {
                return Optional.of(new CrossingEvent(i - decisionIndex, ExitReason.SL_HIT, BacktestConfig.STOP_LOSS_PCT.negate()));
            }
            boolean tpHit = isBuy ? candle.high().compareTo(tpPrice) >= 0 : candle.low().compareTo(tpPrice) <= 0;
            if (tpHit) {
                return Optional.of(new CrossingEvent(i - decisionIndex, ExitReason.TP_HIT, BacktestConfig.TAKE_PROFIT_PCT));
            }
        }
        return Optional.empty();
    }

    /** One decision point's shared TP/SL scan result: how many days forward it resolved, which
     * side crossed first, and the signed return (in call-direction terms) that side represents. */
    record CrossingEvent(int daysForward, ExitReason exitReason, BigDecimal signedReturnPct) {
    }

    private static Optional<HoldGateOutcome> scoreHoldGate(List<Candle> candles, int decisionIndex, BigDecimal decisionClose) {
        int futureIndex = decisionIndex + BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS;
        if (futureIndex >= candles.size()) {
            return Optional.empty();
        }
        BigDecimal pctChange = percentChange(decisionClose, candles.get(futureIndex).close()).abs();
        return Optional.of(pctChange.compareTo(BacktestConfig.LARGE_MOVE_THRESHOLD_PCT) > 0
                ? HoldGateOutcome.LARGE_MOVE : HoldGateOutcome.STABLE);
    }

    private static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
        return to.subtract(from).divide(from, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private static DirectionalOutcomeStats combine(DirectionalOutcomeStats a, DirectionalOutcomeStats b) {
        return new DirectionalOutcomeStats(a.totalCalls() + b.totalCalls(), combineCheckpoint(a.min(), b.min()),
                combineCheckpoint(a.mid(), b.mid()), combineCheckpoint(a.max(), b.max()));
    }

    private static CheckpointStats combineCheckpoint(CheckpointStats a, CheckpointStats b) {
        int win = a.win() + b.win();
        int loss = a.loss() + b.loss();
        double avgWin = win == 0 ? 0.0 : (a.avgWinReturnPct() * a.win() + b.avgWinReturnPct() * b.win()) / win;
        double avgLoss = loss == 0 ? 0.0 : (a.avgLossReturnPct() * a.loss() + b.avgLossReturnPct() * b.loss()) / loss;
        return new CheckpointStats(win, loss, a.wash() + b.wash(), a.notScored() + b.notScored(), avgWin, avgLoss,
                a.tpHit() + b.tpHit(), a.slHit() + b.slHit(), a.horizonExpired() + b.horizonExpired());
    }

    private static final class DirectionalAccumulator {
        int totalCalls;
        final Map<Checkpoint, EnumMap<DirectionalOutcome, Integer>> outcomeCounts = new EnumMap<>(Checkpoint.class);
        final Map<Checkpoint, EnumMap<ExitReason, Integer>> exitReasonCounts = new EnumMap<>(Checkpoint.class);
        final Map<Checkpoint, Integer> notScoredCounts = new EnumMap<>(Checkpoint.class);
        final Map<Checkpoint, BigDecimal> winReturnSums = new EnumMap<>(Checkpoint.class);
        final Map<Checkpoint, BigDecimal> lossReturnSums = new EnumMap<>(Checkpoint.class);

        DirectionalAccumulator() {
            for (Checkpoint checkpoint : Checkpoint.values()) {
                EnumMap<DirectionalOutcome, Integer> counts = new EnumMap<>(DirectionalOutcome.class);
                for (DirectionalOutcome outcome : DirectionalOutcome.values()) {
                    counts.put(outcome, 0);
                }
                outcomeCounts.put(checkpoint, counts);
                EnumMap<ExitReason, Integer> reasons = new EnumMap<>(ExitReason.class);
                for (ExitReason reason : ExitReason.values()) {
                    reasons.put(reason, 0);
                }
                exitReasonCounts.put(checkpoint, reasons);
                notScoredCounts.put(checkpoint, 0);
                winReturnSums.put(checkpoint, BigDecimal.ZERO);
                lossReturnSums.put(checkpoint, BigDecimal.ZERO);
            }
        }

        void record(Checkpoint checkpoint, Optional<DirectionalScoreResult> result) {
            if (result.isEmpty()) {
                notScoredCounts.merge(checkpoint, 1, Integer::sum);
                return;
            }
            DirectionalScoreResult scoreResult = result.get();
            outcomeCounts.get(checkpoint).merge(scoreResult.outcome(), 1, Integer::sum);
            exitReasonCounts.get(checkpoint).merge(scoreResult.exitReason(), 1, Integer::sum);
            if (scoreResult.outcome() == DirectionalOutcome.WIN) {
                winReturnSums.merge(checkpoint, scoreResult.signedReturnPct(), BigDecimal::add);
            } else if (scoreResult.outcome() == DirectionalOutcome.LOSS) {
                lossReturnSums.merge(checkpoint, scoreResult.signedReturnPct(), BigDecimal::add);
            }
        }

        DirectionalOutcomeStats toStats() {
            return new DirectionalOutcomeStats(totalCalls, statsFor(Checkpoint.MIN), statsFor(Checkpoint.MID),
                    statsFor(Checkpoint.MAX));
        }

        private CheckpointStats statsFor(Checkpoint checkpoint) {
            EnumMap<DirectionalOutcome, Integer> counts = outcomeCounts.get(checkpoint);
            int win = counts.get(DirectionalOutcome.WIN);
            int loss = counts.get(DirectionalOutcome.LOSS);
            double avgWin = win == 0 ? 0.0 : winReturnSums.get(checkpoint).doubleValue() / win;
            double avgLoss = loss == 0 ? 0.0 : lossReturnSums.get(checkpoint).doubleValue() / loss;
            EnumMap<ExitReason, Integer> reasons = exitReasonCounts.get(checkpoint);
            return new CheckpointStats(win, loss, counts.get(DirectionalOutcome.WASH), notScoredCounts.get(checkpoint),
                    avgWin, avgLoss, reasons.get(ExitReason.TP_HIT), reasons.get(ExitReason.SL_HIT),
                    reasons.get(ExitReason.HORIZON_EXPIRED));
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
