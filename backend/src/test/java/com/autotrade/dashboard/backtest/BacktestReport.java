package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.signal.HoldTermCalculator;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Aggregate result of one {@link BacktestHarness#run} call: raw call frequency per rule,
 * directional win/loss stats for the four BUY/SELL rules (plus an "overall BUY"/"overall SELL"
 * roll-up combining UNANIMOUS+MAJORITY), and hold-gate large-move stats for the five HOLD rules.
 */
public record BacktestReport(String label, int totalCandles, int totalDecisionPoints,
                              Map<SignalRuleId, Integer> callCounts,
                              Map<SignalRuleId, DirectionalOutcomeStats> directionalStats,
                              DirectionalOutcomeStats overallBuy, DirectionalOutcomeStats overallSell,
                              Map<SignalRuleId, HoldGateStats> holdGateStats,
                              List<BacktestDecisionPoint> buySellDecisionPoints) {

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
        out.printf("BUY/SELL win rate (deadband +/-%s%%) at min/mid/max hold-term day:%n",
                BacktestConfig.WIN_LOSS_DEADBAND_PCT);
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
        out.println("BUY/SELL decision points (spot-check detail):");
        out.printf("  %-25s %-8s %-22s %s%n", "date", "index", "matchedRule", "holdTerm");
        for (BacktestDecisionPoint point : buySellDecisionPoints) {
            out.printf("  %-25s %-8d %-22s %s%n", point.date(), point.index(), point.matchedRule(),
                    point.holdTerm() == null ? "-" : point.holdTerm().label());
        }
    }

    private void printDirectional(PrintStream out, String rowLabel, DirectionalOutcomeStats stats) {
        if (stats == null || stats.totalCalls() == 0) {
            out.printf("  %-22s (n=0)%n", rowLabel);
            return;
        }
        out.printf("  %-22s (n=%d) min %.1f%%(%d scored) mid %.1f%%(%d scored) max %.1f%%(%d scored)%n", rowLabel,
                stats.totalCalls(), stats.min().winRate(), stats.min().scored(), stats.mid().winRate(),
                stats.mid().scored(), stats.max().winRate(), stats.max().scored());
    }
}
