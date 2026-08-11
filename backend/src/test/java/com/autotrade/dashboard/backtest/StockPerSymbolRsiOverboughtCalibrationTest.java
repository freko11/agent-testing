package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;
import com.autotrade.dashboard.signal.SignalRuleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F1-S7: evaluates {@code rsiOverbought} calibration against AAPL — this repo's first stock
 * fixture ({@link FixtureSplits#AAPL}) — using the exact same tune-then-validate-on-its-own-
 * held-out-tail methodology {@code PerSymbolRsiOverboughtCalibrationTest} (E8-F1-S4) established
 * per crypto symbol. AAPL has never been swept before (unlike BTCUSDT/DOGEUSDT, which already had
 * {@code RsiOverboughtRecalibrationTest}'s prior sweep to validate against), so this is a genuine
 * fresh sweep, not a replay of an already-decided candidate — per this story's AC wording
 * ("evaluated... using the same tune/held-out methodology").
 *
 * <p>Same 68-76 candidate grid every RSI-overbought calibration test in this backlog has used,
 * reused verbatim rather than re-derived for a stock. {@code rsiOversold} stays fixed at the
 * current global default (25) throughout, mirroring {@code PerSymbolRsiOverboughtCalibrationTest}
 * — E8-F1-S2 already found it has no measurable BUY-side effect, and that finding was never
 * asset-class-scoped.
 *
 * <p><b>Ship bar:</b> identical to {@code PerSymbolRsiOverboughtCalibrationTest}'s — AAPL gets a
 * {@code PerSymbolRuleThresholds} override only if some candidate's BUY-side
 * ({@code overallBuy().expectancyPctAfterCosts()}) beats the current global default (75, the value
 * AAPL already resolves to today as an unlisted symbol) at every one of MIN/MID/MAX on AAPL's own
 * tuning window, with a comparably large scored {@code n}, <i>and</i> that same candidate still
 * beats 75 at every checkpoint on AAPL's own held-out tail. A no-ship here is a fully legitimate,
 * equally documented outcome per this story's own AC.
 *
 * <p>{@link #sellSideUnaffectedByOverboughtCandidates()} is a structural sanity check, not new
 * evidence — confirms the same RSI-overbought/SELL-side isolation
 * {@code PerSymbolRsiOverboughtCalibrationTest} already established holds for AAPL too.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=StockPerSymbolRsiOverboughtCalibrationTest}) for the actual figures.
 * See docs/CHANGELOG.md's E8-F1-S7 entry for the ship/no-ship decision and figures.
 */
class StockPerSymbolRsiOverboughtCalibrationTest {

    /** Same grid every RSI-overbought calibration test in this backlog has used. */
    private static final List<BigDecimal> CANDIDATE_OVERBOUGHT_VALUES = List.of(
            new BigDecimal("68"), new BigDecimal("70"), new BigDecimal("71"), new BigDecimal("72"),
            new BigDecimal("73"), new BigDecimal("74"), new BigDecimal("75"), new BigDecimal("76"));

    private static final BigDecimal CURRENT_DEFAULT_OVERBOUGHT = SignalRuleEngine.RSI_OVERBOUGHT_THRESHOLD;

    private static final RuleThresholds DEFAULT = RuleThresholds.DEFAULT;

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void sweepAaplOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F1-S7: rsiOverbought swept for AAPL, TUNING WINDOW ONLY (first "
                + FixtureSplits.AAPL_SPLIT_INDEX + " candles) ##########");

        for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
            RuleThresholds candidate = thresholdsFor(overbought);
            runAndPrint("AAPL [tuning]", FixtureSplits.AAPL_TUNING, candidateLabel(overbought), candidate);
        }
    }

    @Test
    void validateAaplOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S7: every rsiOverbought candidate vs. AAPL's own held-out tail ##########");

        for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
            RuleThresholds candidate = thresholdsFor(overbought);
            runAndPrint("AAPL [held-out tail]", FixtureSplits.AAPL_HELD_OUT, candidateLabel(overbought), candidate);
        }
    }

    /** Structural sanity check, not new evidence (see class Javadoc). */
    @Test
    void sellSideUnaffectedByOverboughtCandidates() {
        for (List<Candle> window : List.of(FixtureSplits.AAPL_TUNING, FixtureSplits.AAPL_HELD_OUT)) {
            DirectionalOutcomeStats baselineSell = BacktestHarness.run("AAPL", window, DEFAULT).overallSell();
            for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
                RuleThresholds candidate = thresholdsFor(overbought);
                DirectionalOutcomeStats candidateSell = BacktestHarness.run("AAPL", window, candidate).overallSell();
                assertEquals(baselineSell, candidateSell,
                        "AAPL: overallSell() must be byte-identical to DEFAULT regardless of rsiOverbought (candidate " + overbought + ")");
            }
        }
    }

    private String candidateLabel(BigDecimal overbought) {
        String suffix = overbought.compareTo(CURRENT_DEFAULT_OVERBOUGHT) == 0 ? " (v4/current default)"
                : overbought.compareTo(new BigDecimal("70")) == 0 ? " (pre-tuning)" : "";
        return "25/" + overbought + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal overbought) {
        return new RuleThresholds(SignalRuleEngine.RSI_OVERSOLD_THRESHOLD, overbought,
                DEFAULT.volatilityExtreme(), DEFAULT.volumeDriedUp(), DEFAULT.macdMinHistogramMagnitudePct(),
                DEFAULT.maMinSeparationPctOfPrice());
    }

    private void runAndPrint(String symbolLabel, List<Candle> candles, String thresholdLabel, RuleThresholds thresholds) {
        BacktestReport report = BacktestHarness.run(symbolLabel + " [" + thresholdLabel + "]", candles, thresholds);
        printCompact(report);
        assertStructurallySane(report);
    }

    private void printCompact(BacktestReport report) {
        System.out.println();
        System.out.println(report.label() + ":");
        for (SignalRuleId ruleId : DIRECTIONAL_RULES) {
            printCheckpointLine("  " + ruleId, report.directionalStats().get(ruleId));
        }
        printCheckpointLine("  Overall BUY ", report.overallBuy());
        printCheckpointLine("  Overall SELL", report.overallSell());
    }

    private void printCheckpointLine(String rowLabel, DirectionalOutcomeStats stats) {
        if (stats == null || stats.totalCalls() == 0) {
            System.out.printf("%-30s (n=0)%n", rowLabel);
            return;
        }
        System.out.printf("%-30s min %5.1f%%win exp%+6.3f%%(aft%+6.3f%%)(n=%-3d) | mid %5.1f%%win exp%+6.3f%%(aft%+6.3f%%)(n=%-3d) | max %5.1f%%win exp%+6.3f%%(aft%+6.3f%%)(n=%-3d)%n",
                rowLabel,
                stats.min().winRate(), stats.min().expectancyPct(), stats.min().expectancyPctAfterCosts(), stats.min().scored(),
                stats.mid().winRate(), stats.mid().expectancyPct(), stats.mid().expectancyPctAfterCosts(), stats.mid().scored(),
                stats.max().winRate(), stats.max().expectancyPct(), stats.max().expectancyPctAfterCosts(), stats.max().scored());
    }

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
                assertExpectancySignsAreSane(report.label() + " " + ruleId, stats);
            } else {
                assertEquals(expected, report.holdGateStats().get(ruleId).totalCalls(),
                        report.label() + " " + ruleId + ": hold-gate stats total must match its call count");
            }
        }
        assertExpectancySignsAreSane(report.label() + " Overall BUY", report.overallBuy());
        assertExpectancySignsAreSane(report.label() + " Overall SELL", report.overallSell());
    }

    private void assertExpectancySignsAreSane(String label, DirectionalOutcomeStats stats) {
        for (Checkpoint checkpoint : Checkpoint.values()) {
            CheckpointStats cp = checkpoint == Checkpoint.MIN ? stats.min()
                    : checkpoint == Checkpoint.MID ? stats.mid() : stats.max();
            if (cp.win() > 0) {
                assertTrue(cp.avgWinReturnPct() > 0, label + " " + checkpoint + ": avg win size must be positive");
            }
            if (cp.loss() > 0) {
                assertTrue(cp.avgLossReturnPct() < 0, label + " " + checkpoint + ": avg loss size must be negative");
            }
            assertEquals(cp.scored(), cp.tpHit() + cp.slHit() + cp.horizonExpired(),
                    label + " " + checkpoint + ": tpHit+slHit+horizonExpired must partition scored()");
            assertTrue(cp.expectancyPctAfterCosts() <= cp.expectancyPct(),
                    label + " " + checkpoint + ": after-cost expectancy must never exceed raw expectancy");
        }
    }
}
