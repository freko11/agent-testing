package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.CheckpointStats;
import com.autotrade.dashboard.backtest.DirectionalOutcome;
import com.autotrade.dashboard.backtest.DirectionalScoreResult;
import com.autotrade.dashboard.backtest.ExitReason;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Optional;

/**
 * Win/loss/wash tally at a single fixed horizon (E8-F5-S3) — the single-checkpoint sibling of
 * {@code backtest.DirectionalAccumulator}'s MIN/MID/MAX triple, for decision points that carry no
 * rule-derived hold-term to bracket a range with. Mirrors the test-only {@code BacktestHarness
 * .IndicatorAccumulator}'s own shape and reasoning ("a lone [reading] has no hold-term of its own
 * to derive one from") — that class stays private and test-only and is out of this story's scope
 * to touch, so this is a narrowly-scoped, independent re-implementation of the same idea for
 * {@link WeightedVoteShadowScoringService}'s "weighted-only BUY"/"weighted-only SELL" buckets,
 * where the recorded {@code SignalCallEntry} was HOLD and so carries no persisted hold-term at all.
 *
 * <p>Package-private: an internal accumulation detail of {@link WeightedVoteShadowScoringService},
 * not part of this package's public reporting surface (unlike {@link WeightedVoteBucketOutcome},
 * which exposes the resulting {@link CheckpointStats} on the report itself).
 */
final class WeightedVoteSingleHorizonAccumulator {

    private final EnumMap<DirectionalOutcome, Integer> outcomeCounts = new EnumMap<>(DirectionalOutcome.class);
    private final EnumMap<ExitReason, Integer> exitReasonCounts = new EnumMap<>(ExitReason.class);
    private int notScored;
    private BigDecimal winReturnSum = BigDecimal.ZERO;
    private BigDecimal lossReturnSum = BigDecimal.ZERO;
    private long holdingDaysSum;

    WeightedVoteSingleHorizonAccumulator() {
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
        return new CheckpointStats(win, loss, wash, notScored, avgWin, avgLoss,
                exitReasonCounts.get(ExitReason.TP_HIT), exitReasonCounts.get(ExitReason.SL_HIT),
                exitReasonCounts.get(ExitReason.HORIZON_EXPIRED), avgHoldingDays);
    }
}
