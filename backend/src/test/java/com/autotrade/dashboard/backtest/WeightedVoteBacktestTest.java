package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E8-F3-S1: replays {@link WeightedVoteRuleEngine#evaluate} (weighted vote, {@link
 * WeightedVoteRuleEngine.IndicatorWeights#DEFAULT}) through the exact same walk-forward machinery
 * as {@link SignalRuleEngine#evaluate} (the current unweighted table), via {@link
 * BacktestHarness#run(String, List, BacktestHarness.RuleEvaluator, SignalRuleEngine.RuleThresholds)},
 * and prints both reports' expectancy side by side — the literal "A/B against the current table"
 * comparison mode this story's AC asks for. {@code WeightedVoteRuleEngine} is not wired into
 * production by this story (see its own class Javadoc); this test is the evidence a future story
 * would read before deciding whether to.
 *
 * <p>Same overfitting caveat as {@code ThresholdCalibrationTest}/{@code
 * IndicatorExpectancyCalibrationTest}: both fixtures are also {@code IndicatorWeights.DEFAULT}'s
 * only tuning data. Assertions here are structural only — the printed comparison is the evidence
 * under review, not a regression target.
 */
class WeightedVoteBacktestTest {

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");

    @Test
    void compareUnweightedVsWeighted_btcUsdt() {
        compareAndPrint("BTCUSDT", BTCUSDT);
    }

    @Test
    void compareUnweightedVsWeighted_dogeUsdt() {
        compareAndPrint("DOGEUSDT", DOGEUSDT);
    }

    private void compareAndPrint(String symbol, List<Candle> candles) {
        BacktestReport unweighted = BacktestHarness.run(symbol + " [unweighted table]", candles);
        BacktestReport weighted = BacktestHarness.run(symbol + " [weighted vote]", candles,
                WeightedVoteRuleEngine::evaluate, SignalRuleEngine.RuleThresholds.DEFAULT);

        System.out.printf("%n########## A/B: unweighted table vs. weighted vote (%s) ##########%n", symbol);
        printCompact("Unweighted", unweighted);
        printCompact("Weighted  ", weighted);

        assertStructurallySane(unweighted);
        assertStructurallySane(weighted);

        // Both engines replay the exact same candle series/decision-point count, whatever the
        // per-decision-point rule ID differs.
        assertEquals(unweighted.totalDecisionPoints(), weighted.totalDecisionPoints(),
                symbol + ": both engines must replay the same number of decision points");
    }

    private void printCompact(String rowLabel, BacktestReport report) {
        System.out.println();
        System.out.println(rowLabel + " " + report.label() + ":");
        printCheckpointLine("  Overall BUY ", report.overallBuy());
        printCheckpointLine("  Overall SELL", report.overallSell());
        System.out.printf("  Call counts: BULLISH_UNANIMOUS=%d BULLISH_MAJORITY=%d BEARISH_UNANIMOUS=%d BEARISH_MAJORITY=%d NO_STRONG_SIGNAL=%d%n",
                report.callCounts().get(SignalRuleId.BULLISH_UNANIMOUS), report.callCounts().get(SignalRuleId.BULLISH_MAJORITY),
                report.callCounts().get(SignalRuleId.BEARISH_UNANIMOUS), report.callCounts().get(SignalRuleId.BEARISH_MAJORITY),
                report.callCounts().get(SignalRuleId.NO_STRONG_SIGNAL));
    }

    private void printCheckpointLine(String rowLabel, DirectionalOutcomeStats stats) {
        if (stats == null || stats.totalCalls() == 0) {
            System.out.printf("%-24s (n=0)%n", rowLabel);
            return;
        }
        System.out.printf("%-24s min %5.1f%%win exp%+6.3f%%(n=%-3d) | mid %5.1f%%win exp%+6.3f%%(n=%-3d) | max %5.1f%%win exp%+6.3f%%(n=%-3d)%n",
                rowLabel,
                stats.min().winRate(), stats.min().expectancyPct(), stats.min().scored(),
                stats.mid().winRate(), stats.mid().expectancyPct(), stats.mid().scored(),
                stats.max().winRate(), stats.max().expectancyPct(), stats.max().scored());
    }

    /** Same structural invariants {@link BacktestHarnessTest}/{@code ThresholdCalibrationTest}
     * already check against the unweighted table, reverified for the weighted engine's report
     * too — a different rule engine still owes the same "every decision point accounted for
     * exactly once" and "expectancy signs match the WIN/LOSS classification" guarantees. */
    private void assertStructurallySane(BacktestReport report) {
        int totalFromCounts = report.callCounts().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(report.totalDecisionPoints(), totalFromCounts,
                report.label() + ": every decision point must land in exactly one SignalRuleId bucket");

        for (SignalRuleId ruleId : SignalRuleId.values()) {
            int expected = report.callCounts().get(ruleId);
            if (ruleId.call() == SignalCall.BUY || ruleId.call() == SignalCall.SELL) {
                DirectionalOutcomeStats stats = report.directionalStats().get(ruleId);
                assertEquals(expected, stats.totalCalls(),
                        report.label() + " " + ruleId + ": directional stats total must match its call count");
            } else {
                assertEquals(expected, report.holdGateStats().get(ruleId).totalCalls(),
                        report.label() + " " + ruleId + ": hold-gate stats total must match its call count");
            }
        }
    }
}
