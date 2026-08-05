package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.signal.HoldTermCalculator;
import com.autotrade.dashboard.signal.IndicatorId;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Aggregate result of one {@link BacktestHarness#run} call: raw call frequency per rule,
 * directional win/loss/expectancy stats (E2-F4-S1/E2-F4-S2) for the four BUY/SELL rules (plus an
 * "overall BUY"/"overall SELL" roll-up combining UNANIMOUS+MAJORITY), hold-gate large-move stats
 * for the five HOLD rules, and each individual indicator's own directional-read expectancy
 * (E8-F3-S1's {@code indicatorStats}) — the evidence {@code WeightedVoteRuleEngine.IndicatorWeights}
 * is calibrated from.
 */
public record BacktestReport(String label, int totalCandles, int totalDecisionPoints,
                              Map<SignalRuleId, Integer> callCounts,
                              Map<SignalRuleId, DirectionalOutcomeStats> directionalStats,
                              DirectionalOutcomeStats overallBuy, DirectionalOutcomeStats overallSell,
                              Map<SignalRuleId, HoldGateStats> holdGateStats,
                              List<BacktestDecisionPoint> buySellDecisionPoints,
                              Map<IndicatorId, CheckpointStats> indicatorStats) {

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    private static final List<SignalRuleId> HOLD_RULES = List.of(SignalRuleId.VOLATILITY_TOO_EXTREME,
            SignalRuleId.VOLUME_DRIED_UP, SignalRuleId.NO_VOLUME_DATA, SignalRuleId.CONFLICTING_SIGNALS,
            SignalRuleId.NO_STRONG_SIGNAL);

    public void printTo(PrintStream out) {
        out.printf("%n=== Backtest: %s (%d candles, %d decision points) ===%n", label, totalCandles, totalDecisionPoints);
        out.println("Rule table " + SignalRuleEngine.RULE_TABLE_VERSION + " / hold-term table "
                + HoldTermCalculator.HOLD_TERM_TABLE_VERSION);

        out.println();
        out.println("Call counts:");
        for (SignalRuleId ruleId : SignalRuleId.values()) {
            out.printf("  %-22s %d%n", ruleId, callCounts.getOrDefault(ruleId, 0));
        }

        out.println();
        out.printf("BUY/SELL win rate + expectancy (avg win/loss size, deadband +/-%s%%, round-trip cost %sbps) at min/mid/max hold-term day:%n",
                BacktestConfig.WIN_LOSS_DEADBAND_PCT, BacktestConfig.TRANSACTION_COST_BPS);
        for (SignalRuleId ruleId : DIRECTIONAL_RULES) {
            printDirectional(out, ruleId.name(), directionalStats.get(ruleId));
        }
        printDirectional(out, "Overall BUY", overallBuy);
        printDirectional(out, "Overall SELL", overallSell);

        out.println();
        out.printf("HOLD-gate check (>%s%% move over %d-day reference horizon):%n",
                BacktestConfig.LARGE_MOVE_THRESHOLD_PCT, BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS);
        for (SignalRuleId ruleId : HOLD_RULES) {
            HoldGateStats stats = holdGateStats.get(ruleId);
            out.printf("  %-22s (n=%d, scored=%d): %.1f%% large-move%n", ruleId, stats.totalCalls(),
                    stats.scoredCount(), stats.largeMoveRate());
        }

        out.println();
        out.printf("BUY/SELL decision points (spot-check detail, exit reason at max checkpoint, TP/SL +%s%%/-%s%%):%n",
                BacktestConfig.TAKE_PROFIT_PCT, BacktestConfig.STOP_LOSS_PCT);
        out.printf("  %-25s %-8s %-22s %-12s %s%n", "date", "index", "matchedRule", "holdTerm", "maxExit");
        for (BacktestDecisionPoint point : buySellDecisionPoints) {
            String maxExit = point.maxResult().map(r -> r.exitReason().name()).orElse("-");
            out.printf("  %-25s %-8d %-22s %-12s %s%n", point.date(), point.index(), point.matchedRule(),
                    point.holdTerm() == null ? "-" : point.holdTerm().label(), maxExit);
        }

        printIndicatorExpectancy(out);
    }

    /** E8-F3-S1: each indicator's own directional-read win rate/expectancy, scored independently
     * of which combined rule (if any) matched — the per-indicator evidence a weighted vote's
     * weights are calibrated from. Reuses {@link #printCheckpoint}'s single-line format. */
    private void printIndicatorExpectancy(PrintStream out) {
        out.println();
        out.printf("Per-indicator expectancy (own directional read, %d-day reference horizon, deadband +/-%s%%, round-trip cost %sbps):%n",
                BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS, BacktestConfig.WIN_LOSS_DEADBAND_PCT,
                BacktestConfig.TRANSACTION_COST_BPS);
        for (IndicatorId indicatorId : IndicatorId.values()) {
            CheckpointStats stats = indicatorStats.get(indicatorId);
            out.printf("  %-22s (n=%d)%n", indicatorId, stats.win() + stats.loss() + stats.wash() + stats.notScored());
            printCheckpoint(out, "  -", stats);
        }
    }

    private void printDirectional(PrintStream out, String rowLabel, DirectionalOutcomeStats stats) {
        if (stats == null || stats.totalCalls() == 0) {
            out.printf("  %-22s (n=0)%n", rowLabel);
            return;
        }
        out.printf("  %-22s (n=%d)%n", rowLabel, stats.totalCalls());
        printCheckpoint(out, "min", stats.min());
        printCheckpoint(out, "mid", stats.mid());
        printCheckpoint(out, "max", stats.max());
    }

    private void printCheckpoint(PrintStream out, String checkpointLabel, CheckpointStats cp) {
        out.printf("    %-3s %5.1f%% win (%d scored) | avg win %+6.2f%% | avg loss %+6.2f%% | expectancy %+6.3f%% (after costs %+6.3f%%)"
                        + " | tpHit=%d slHit=%d horizonExpired=%d%n",
                checkpointLabel, cp.winRate(), cp.scored(), cp.avgWinReturnPct(), cp.avgLossReturnPct(),
                cp.expectancyPct(), cp.expectancyPctAfterCosts(), cp.tpHit(), cp.slHit(), cp.horizonExpired());
    }
}
