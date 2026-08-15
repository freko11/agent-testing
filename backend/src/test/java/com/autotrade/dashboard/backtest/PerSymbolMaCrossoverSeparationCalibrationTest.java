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
 * E8-F1-S10: calibrates {@code maMinSeparationPctOfPrice} <b>per ticker symbol</b> instead of as
 * one global value — the same per-symbol mechanism E8-F1-S8 applied to
 * {@code macdMinHistogramMagnitudePct}, this time for the MA-crossover axis. E8-F1-S6's own
 * global sweep ({@code MaCrossoverSeparationCalibrationTest}) already found the same
 * asset-divergent, no-single-value-wins-everywhere conflict on the BUY side that every prior
 * global-bar E8-F1 axis hit: BTCUSDT prefers ~1.00% separation, DOGEUSDT prefers ~2.00%, SOLUSDT
 * prefers no filter at all.
 *
 * <p>Same independent-per-symbol tune/held-out design {@code PerSymbolRsiOverboughtCalibrationTest}
 * (E8-F1-S4) and {@code PerSymbolMacdHistogramMagnitudeCalibrationTest} (E8-F1-S8) established:
 * <ol>
 *   <li>{@link #sweepEachSymbolOnItsOwnTuningWindow()} sweeps the same 8-candidate grid
 *   {@code MaCrossoverSeparationCalibrationTest} used (0.00% through 10.00%) against each of
 *   BTCUSDT/DOGEUSDT/SOLUSDT's own first {@link FixtureSplits#SPLIT_INDEX} candles only — never
 *   that symbol's own held-out tail, never another symbol's data. {@code rsiOversold}/{@code
 *   rsiOverbought} and {@code macdMinHistogramMagnitudePct} stay fixed at the current global
 *   default (25/75/0) throughout — NOT SOLUSDT's own shipped {@code macdMinHistogramMagnitudePct
 *   = 0.10} override, since this test calibrates the global-default baseline for the MA axis in
 *   isolation, matching how E8-F1-S8's own template fixed the *other* axes at global default
 *   too.</li>
 *   <li>{@link #validateEachSymbolOnItsOwnHeldOutTail()} replays every candidate for a symbol
 *   against that <b>same</b> symbol's own held-out tail (candles 700-1000) — never a different
 *   symbol's tail.</li>
 * </ol>
 *
 * <p><b>Ship bar, per symbol independently (E8-F1-S4/S8's bar, not {@code
 * MaCrossoverSeparationCalibrationTest}'s all-three-simultaneous one — that difference is exactly
 * why the earlier global sweep no-shipped everywhere):</b> a symbol gets a {@code
 * PerSymbolRuleThresholds} override only if (a) some candidate's BUY-side ({@code
 * overallBuy().expectancyPctAfterCosts()}) beats the {@code separation=0} baseline at every one of
 * MIN/MID/MAX on that symbol's own tuning window, with a comparably large scored {@code n}, and
 * (b) that same candidate's BUY-side still beats the {@code separation=0} baseline at every
 * checkpoint on that symbol's own held-out tail. A symbol whose tuning window produces no such
 * winner, or whose winner doesn't confirm on its own held-out tail, ships no override and falls
 * back to {@link RuleThresholds#DEFAULT} (separation 0) — a legitimate per-symbol no-ship, same
 * treatment E8-F1-S4/S8 gave their own no-ship outcomes.
 *
 * <p>{@code overallSell()} is printed for every candidate (see {@link #printCompact}) but never
 * gates the ship decision here — acting on a SELL-side effect is E8-F1-S11's separate, chartered
 * story (mirroring how E8-F1-S9 was chartered separately from E8-F1-S8). See
 * docs/CHANGELOG.md's E8-F1-S10 entry and {@link com.autotrade.dashboard.signal.PerSymbolRuleThresholds}'s
 * own class Javadoc for the actual per-symbol result, including any documented SELL-side effect of
 * whatever shipped.
 *
 * <p>Assertions here are structural only (plus any pinned SELL-side effect for a symbol that
 * actually ships, mirroring E8-F1-S8's {@code shippedSolusdtCandidateAlsoImprovesSellSide}
 * pattern), mirroring every other E8 calibration test — the printed report is the evidence the
 * ship decision above was actually made from. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=PerSymbolMaCrossoverSeparationCalibrationTest}) for the full figures.
 */
class PerSymbolMaCrossoverSeparationCalibrationTest {

    /** Same grid {@code MaCrossoverSeparationCalibrationTest} (E8-F1-S6) swept globally, reused
     * verbatim rather than re-deriving a new grid per symbol. */
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
        System.out.println("########## E8-F1-S10: maMinSeparationPctOfPrice swept per symbol, TUNING WINDOW ONLY (first "
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
        System.out.println("########## E8-F1-S10: every maMinSeparationPctOfPrice candidate vs. that SAME symbol's own held-out tail ##########");

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
