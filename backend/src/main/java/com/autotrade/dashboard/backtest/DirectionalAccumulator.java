package com.autotrade.dashboard.backtest;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Win/loss/wash tally across all three {@link Checkpoint}s for one directional (BUY or SELL)
 * bucket (E2-F4-S1/S2, E8-F2-S1). Promoted to main scope and top-level (E8-F5-S1) from {@code
 * BacktestHarness}'s private nested class of the same name/shape, so {@code
 * monitoring.LiveSignalDriftService} can reuse the exact same accumulation logic — scoring real
 * forward market data instead of a checked-in fixture, but with the identical MIN/MID/MAX
 * win/loss/expectancy bookkeeping. {@code BacktestHarness} itself keeps using this class
 * unchanged for its own per-rule/per-regime accumulation (same package, no import needed).
 */
public final class DirectionalAccumulator {

    public int totalCalls;

    private final Map<Checkpoint, EnumMap<DirectionalOutcome, Integer>> outcomeCounts = new EnumMap<>(Checkpoint.class);
    private final Map<Checkpoint, EnumMap<ExitReason, Integer>> exitReasonCounts = new EnumMap<>(Checkpoint.class);
    private final Map<Checkpoint, Integer> notScoredCounts = new EnumMap<>(Checkpoint.class);
    private final Map<Checkpoint, BigDecimal> winReturnSums = new EnumMap<>(Checkpoint.class);
    private final Map<Checkpoint, BigDecimal> lossReturnSums = new EnumMap<>(Checkpoint.class);

    public DirectionalAccumulator() {
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

    public void record(Checkpoint checkpoint, Optional<DirectionalScoreResult> result) {
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

    public DirectionalOutcomeStats toStats() {
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
