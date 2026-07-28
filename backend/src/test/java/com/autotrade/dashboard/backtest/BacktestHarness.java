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

            SignalRuleId matchedRule = SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend);
            HoldTerm holdTerm = HoldTermCalculator.calculate(matchedRule, volatility);

            callCounts.merge(matchedRule, 1, Integer::sum);
            BigDecimal decisionClose = candles.get(i).close();

            if (holdTerm != null) {
                DirectionalAccumulator acc = directional.get(matchedRule);
                acc.totalCalls++;
                boolean isBuy = matchedRule.call() == SignalCall.BUY;
                int midDays = (int) Math.round((holdTerm.minDays() + holdTerm.maxDays()) / 2.0);

                acc.record(Checkpoint.MIN, score(candles, i, holdTerm.minDays(), decisionClose, isBuy));
                acc.record(Checkpoint.MID, score(candles, i, midDays, decisionClose, isBuy));
                acc.record(Checkpoint.MAX, score(candles, i, holdTerm.maxDays(), decisionClose, isBuy));

                buySellPoints.add(new BacktestDecisionPoint(i, candles.get(i).timestamp(), rsi, macd.histogram(),
                        volatility, volumeTrend, matchedRule, holdTerm));
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

    /** @return empty if {@code daysForward} candles past the decision point don't exist in the fixture. */
    private static Optional<DirectionalOutcome> score(List<Candle> candles, int decisionIndex, int daysForward,
                                                        BigDecimal decisionClose, boolean isBuy) {
        int futureIndex = decisionIndex + daysForward;
        if (futureIndex >= candles.size()) {
            return Optional.empty();
        }
        BigDecimal pctChange = percentChange(decisionClose, candles.get(futureIndex).close());
        BigDecimal signedForCall = isBuy ? pctChange : pctChange.negate();

        if (signedForCall.abs().compareTo(BacktestConfig.WIN_LOSS_DEADBAND_PCT) <= 0) {
            return Optional.of(DirectionalOutcome.WASH);
        }
        return Optional.of(signedForCall.signum() > 0 ? DirectionalOutcome.WIN : DirectionalOutcome.LOSS);
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
        return new CheckpointStats(a.win() + b.win(), a.loss() + b.loss(), a.wash() + b.wash(),
                a.notScored() + b.notScored());
    }

    private static final class DirectionalAccumulator {
        int totalCalls;
        final Map<Checkpoint, EnumMap<DirectionalOutcome, Integer>> outcomeCounts = new EnumMap<>(Checkpoint.class);
        final Map<Checkpoint, Integer> notScoredCounts = new EnumMap<>(Checkpoint.class);

        DirectionalAccumulator() {
            for (Checkpoint checkpoint : Checkpoint.values()) {
                EnumMap<DirectionalOutcome, Integer> counts = new EnumMap<>(DirectionalOutcome.class);
                for (DirectionalOutcome outcome : DirectionalOutcome.values()) {
                    counts.put(outcome, 0);
                }
                outcomeCounts.put(checkpoint, counts);
                notScoredCounts.put(checkpoint, 0);
            }
        }

        void record(Checkpoint checkpoint, Optional<DirectionalOutcome> outcome) {
            if (outcome.isEmpty()) {
                notScoredCounts.merge(checkpoint, 1, Integer::sum);
            } else {
                outcomeCounts.get(checkpoint).merge(outcome.get(), 1, Integer::sum);
            }
        }

        DirectionalOutcomeStats toStats() {
            return new DirectionalOutcomeStats(totalCalls, statsFor(Checkpoint.MIN), statsFor(Checkpoint.MID),
                    statsFor(Checkpoint.MAX));
        }

        private CheckpointStats statsFor(Checkpoint checkpoint) {
            EnumMap<DirectionalOutcome, Integer> counts = outcomeCounts.get(checkpoint);
            return new CheckpointStats(counts.get(DirectionalOutcome.WIN), counts.get(DirectionalOutcome.LOSS),
                    counts.get(DirectionalOutcome.WASH), notScoredCounts.get(checkpoint));
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
