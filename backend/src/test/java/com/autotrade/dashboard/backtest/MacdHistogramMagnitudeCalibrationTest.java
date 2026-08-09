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
 * E8-F1-S5: a BUY-side calibration attempt on a non-RSI axis, following up on E8-F1-S2/S3's
 * finding that neither RSI bound, adjusted alone, fixes E8-F4-S1's still-open BUY-side
 * out-of-sample mismatch. Those two stories named MACD/MA-crossover thresholds as the one
 * untried mechanism short of E8-F1-S4's per-symbol RSI override; this story tries MACD only —
 * MA-crossover thresholding is an explicit out-of-scope follow-up, only warranted if this one
 * doesn't resolve the mismatch, mirroring the rsiOversold-then-rsiOverbought split across
 * E8-F1-S2/S3.
 *
 * <p>{@link SignalRuleEngine.RuleThresholds#macdMinHistogramMagnitudePct} is new production
 * surface this story adds (default 0, reproducing today's any-nonzero-crossover behavior
 * exactly): a probe run of {@code MacdCalculator} against all three fixtures'
 * {@link FixtureSplits#SPLIT_INDEX}-candle tuning windows found real {@code histogramPctOfPrice}
 * values ranging from a few thousandths of a percent up to roughly 2.6% (BTCUSDT), 6.0%
 * (DOGEUSDT), and 4.9% (SOLUSDT) at the extremes, with medians of 0.54%/1.03%/1.09% respectively
 * — informing {@link #CANDIDATE_MAGNITUDE_VALUES} below, which spans from "no filter" up through
 * comfortably past each symbol's own median.
 *
 * <p>Same independent-per-symbol tune/held-out design {@code PerSymbolRsiOverboughtCalibrationTest}
 * (E8-F1-S4) established, reusing its exact {@link FixtureSplits} 70/30 split rather than a new
 * one:
 * <ol>
 *   <li>{@link #sweepEachSymbolOnItsOwnTuningWindow()} sweeps the candidate grid against each of
 *   BTCUSDT/DOGEUSDT/SOLUSDT's own first {@link FixtureSplits#SPLIT_INDEX} candles only.</li>
 *   <li>{@link #validateEachSymbolOnItsOwnHeldOutTail()} replays every candidate for a symbol
 *   against that same symbol's own held-out tail (candles 700-1000).</li>
 * </ol>
 *
 * <p><b>Ship bar:</b> unlike E8-F1-S4 (which shipped independent per-symbol overrides via {@code
 * PerSymbolRuleThresholds}), this story's AC adds a single new field to the global {@code
 * RuleThresholds} record, not a per-symbol mechanism — so the bar is the same all-surfaces bar
 * {@code RsiOverboughtRecalibrationTest} (E8-F1-S3) used: a candidate only ships if its BUY-side
 * ({@code overallBuy().expectancyPctAfterCosts()}) beats the {@code magnitude=0} baseline at
 * every one of MIN/MID/MAX, with a comparably large scored {@code n}, on <b>all three</b>
 * symbols' own held-out tails simultaneously. The SELL side is checked too (same as every prior
 * E8-F1 recalibration test) — a BUY-side fix that quietly breaks SELL would not be a net
 * improvement. See docs/CHANGELOG.md's E8-F1-S5 entry for the actual result and decision.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=MacdHistogramMagnitudeCalibrationTest}) for the actual figures.
 */
class MacdHistogramMagnitudeCalibrationTest {

    /** Spans "no filter" (current production behavior) up through roughly one standard swing past
     * each symbol's own tuning-window median (0.54%/1.03%/1.09%) established by this story's probe
     * run — see class Javadoc. */
    private static final List<BigDecimal> CANDIDATE_MAGNITUDE_VALUES = List.of(
            new BigDecimal("0.00"), new BigDecimal("0.10"), new BigDecimal("0.25"), new BigDecimal("0.50"),
            new BigDecimal("0.75"), new BigDecimal("1.00"), new BigDecimal("1.50"), new BigDecimal("2.00"));

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
        System.out.println("########## E8-F1-S5: macdMinHistogramMagnitudePct swept per symbol, TUNING WINDOW ONLY (first "
                + FixtureSplits.SPLIT_INDEX + " candles each) ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [tuning] ----");
            for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
                RuleThresholds candidate = thresholdsFor(magnitude);
                runAndPrint(symbol.name() + " [tuning]", symbol.tuning(), candidateLabel(magnitude), candidate);
            }
        }
    }

    @Test
    void validateEachSymbolOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S5: every macdMinHistogramMagnitudePct candidate vs. that SAME symbol's own held-out tail ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
                RuleThresholds candidate = thresholdsFor(magnitude);
                runAndPrint(symbol.name() + " [held-out tail]", symbol.heldOut(), candidateLabel(magnitude), candidate);
            }
        }
    }

    private String candidateLabel(BigDecimal magnitude) {
        String suffix = magnitude.compareTo(BigDecimal.ZERO) == 0 ? " (current default, no filter)" : "";
        return "macd>=" + magnitude + "%" + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal magnitude) {
        return new RuleThresholds(DEFAULT.rsiOversold(), DEFAULT.rsiOverbought(), DEFAULT.volatilityExtreme(),
                DEFAULT.volumeDriedUp(), magnitude);
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
