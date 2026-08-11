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
 * E8-F1-S6: a BUY-side calibration attempt on the one axis E8-F1-S5 named as the remaining
 * untried mechanism short of E8-F1-S4's per-symbol RSI override — MA-crossover thresholding.
 * E8-F1-S2/S3 (RSI bounds) and E8-F1-S5 (MACD histogram magnitude) each failed to fix the
 * BUY-side out-of-sample mismatch E8-F4-S1 flagged, uniformly across all three symbols; this
 * story tries the last named axis.
 *
 * <p>{@link SignalRuleEngine.RuleThresholds#maMinSeparationPctOfPrice} is new production surface
 * this story adds (default 0, reproducing today's any-crossover-counts behavior exactly): a probe
 * run of {@code MovingAverageCrossoverCalculator} against all three fixtures'
 * {@link FixtureSplits#SPLIT_INDEX}-candle tuning windows found real {@code separationPctOfPrice}
 * values ranging from a few thousandths of a percent up to roughly 13.66% (BTCUSDT), 36.19%
 * (DOGEUSDT), and 23.94% (SOLUSDT) at the extremes, with medians of 3.20%/6.97%/6.54%
 * respectively — considerably larger than E8-F1-S5's MACD-histogram medians (0.54%/1.03%/1.09%),
 * consistent with a 10-vs-30-period SMA gap being a coarser signal than a MACD histogram at the
 * same horizon. This informs {@link #CANDIDATE_SEPARATION_VALUES} below, which spans from "no
 * filter" up through comfortably past each symbol's own median.
 *
 * <p>Same independent-per-symbol tune/held-out design {@code PerSymbolRsiOverboughtCalibrationTest}
 * (E8-F1-S4) and {@code MacdHistogramMagnitudeCalibrationTest} (E8-F1-S5) established, reusing
 * {@link FixtureSplits}'s exact 70/30 split rather than a new one:
 * <ol>
 *   <li>{@link #sweepEachSymbolOnItsOwnTuningWindow()} sweeps the candidate grid against each of
 *   BTCUSDT/DOGEUSDT/SOLUSDT's own first {@link FixtureSplits#SPLIT_INDEX} candles only.</li>
 *   <li>{@link #validateEachSymbolOnItsOwnHeldOutTail()} replays every candidate for a symbol
 *   against that same symbol's own held-out tail (candles 700-1000).</li>
 * </ol>
 *
 * <p><b>Ship bar:</b> the same all-surfaces bar {@code RsiOverboughtRecalibrationTest} (E8-F1-S3)
 * and {@code MacdHistogramMagnitudeCalibrationTest} (E8-F1-S5) used — a candidate only ships if
 * its BUY-side ({@code overallBuy().expectancyPctAfterCosts()}) beats the {@code magnitude=0}
 * baseline at every one of MIN/MID/MAX, with a comparably large scored {@code n}, on <b>all
 * three</b> symbols' own held-out tails simultaneously. The SELL side is checked too (same as
 * every prior E8-F1 recalibration test) — a BUY-side fix that quietly breaks SELL would not be a
 * net improvement, and (per E8-F1-S5's own precedent) a SELL-only gain is noted but not acted on,
 * since this story is chartered for the BUY-side mismatch specifically. See docs/CHANGELOG.md's
 * E8-F1-S6 entry for the actual result and decision.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=MaCrossoverSeparationCalibrationTest}) for the actual figures.
 */
class MaCrossoverSeparationCalibrationTest {

    /** Spans "no filter" (current production behavior) up through comfortably past each symbol's
     * own tuning-window median (3.20%/6.97%/6.54%) established by this story's probe run — see
     * class Javadoc. */
    private static final List<BigDecimal> CANDIDATE_SEPARATION_VALUES = List.of(
            new BigDecimal("0.00"), new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"),
            new BigDecimal("4.00"), new BigDecimal("5.00"), new BigDecimal("7.00"), new BigDecimal("10.00"));

    private static final RuleThresholds DEFAULT = RuleThresholds.DEFAULT;

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    @Test
    void sweepEachSymbolOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F1-S6: maMinSeparationPctOfPrice swept per symbol, TUNING WINDOW ONLY (first "
                + FixtureSplits.SPLIT_INDEX + " candles each) ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [tuning] ----");
            for (BigDecimal separation : CANDIDATE_SEPARATION_VALUES) {
                RuleThresholds candidate = thresholdsFor(separation);
                runAndPrint(symbol.name() + " [tuning]", symbol.tuning(), candidateLabel(separation), candidate);
            }
        }
    }

    @Test
    void validateEachSymbolOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S6: every maMinSeparationPctOfPrice candidate vs. that SAME symbol's own held-out tail ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal separation : CANDIDATE_SEPARATION_VALUES) {
                RuleThresholds candidate = thresholdsFor(separation);
                runAndPrint(symbol.name() + " [held-out tail]", symbol.heldOut(), candidateLabel(separation), candidate);
            }
        }
    }

    private String candidateLabel(BigDecimal separation) {
        String suffix = separation.compareTo(BigDecimal.ZERO) == 0 ? " (current default, no filter)" : "";
        return "ma>=" + separation + "%" + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal separation) {
        return new RuleThresholds(DEFAULT.rsiOversold(), DEFAULT.rsiOverbought(), DEFAULT.volatilityExtreme(),
                DEFAULT.volumeDriedUp(), DEFAULT.macdMinHistogramMagnitudePct(), separation);
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

    /** Prints both raw and after-cost expectancy per checkpoint — this story's ship bar is decided
     * on {@code expectancyPctAfterCosts()} specifically, so it needs to be visible inline. */
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

    /** Same structural invariants every other E8 calibration test checks, reapplied per candidate
     * since a threshold shift changes bucket membership. */
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
