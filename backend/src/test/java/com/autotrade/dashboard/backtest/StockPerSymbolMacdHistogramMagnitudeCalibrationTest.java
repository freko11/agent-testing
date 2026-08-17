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
 * E8-F1-S12: evaluates {@code macdMinHistogramMagnitudePct} calibration against AAPL - this
 * repo's only stock fixture - using the exact same tune-then-validate-on-its-own-held-out-tail
 * methodology {@code PerSymbolMacdHistogramMagnitudeCalibrationTest} (E8-F1-S8) established per
 * crypto symbol. This axis shipped a real per-symbol override for SOLUSDT (E8-F1-S8) but was
 * never evaluated against AAPL - E8-F1-S7 (this repo's stock-evidence story) only ever swept
 * {@code rsiOverbought} and the SELL-side regime gate, not this axis. Filed after a sweep for
 * flagged-but-unactioned findings found the gap: both this axis and the MA-crossover-separation
 * SELL gate (E8-F1-S11) shipped real, currently crypto-only production changes purely "for lack
 * of stock evidence," not because stock evidence contradicted them.
 *
 * <p>A throwaway probe of {@code MacdCalculator} against AAPL's own tuning window (median
 * {@code histogramPctOfPrice} 0.30%, p90 0.99%, max 2.52%) confirmed the existing 0.00%-2.00%
 * grid every crypto MACD-magnitude calibration test in this backlog has used remains
 * appropriately sized for AAPL - its span comfortably covers AAPL's own p90 with room past it,
 * the same "no filter through comfortably past the median" shape the grid was originally sized
 * for on the crypto fixtures - so it's reused verbatim rather than re-derived. {@code
 * rsiOversold}/{@code rsiOverbought} stay fixed at the current global default (25/75) throughout,
 * as does {@code maMinSeparationPctOfPrice} (0), mirroring {@code
 * PerSymbolMacdHistogramMagnitudeCalibrationTest}'s own fixed-other-axes design.
 *
 * <p><b>Ship bar:</b> identical to {@code PerSymbolMacdHistogramMagnitudeCalibrationTest}'s own
 * per-symbol bar - AAPL gets a {@code PerSymbolRuleThresholds} override only if some candidate's
 * BUY-side ({@code overallBuy().expectancyPctAfterCosts()}) beats the {@code magnitude=0}
 * baseline at every one of MIN/MID/MAX on AAPL's own tuning window, with a comparably large
 * scored {@code n}, <i>and</i> that same candidate still beats the baseline at every checkpoint
 * on AAPL's own held-out tail. A no-ship here is a fully legitimate, equally documented outcome,
 * per this backlog's own precedent (most axes swept against most symbols come back no-ship).
 *
 * <p><b>Actual result of the real run: no ship.</b> AAPL's tuning window does produce two
 * candidates that beat the magnitude=0 baseline's BUY-side after-cost expectancy at all three
 * checkpoints simultaneously - macd&gt;=0.50% (baseline min/mid/max -0.256%/-0.036%/+0.216%,
 * n=202 to -0.234%/+0.052%/+0.479%, n=69) and macd&gt;=0.75% (to +0.220%/+0.440%/+0.821%,
 * n=38) - but neither confirms on AAPL's own held-out tail. macd&gt;=0.50% fails specifically at
 * the MIN checkpoint (held-out baseline +0.111%/+0.189%/+0.279%, n=95 to candidate
 * -0.015%/+0.296%/+0.584%, n=39 - MID/MAX still improve, only MIN reverses), the same partial-miss
 * shape {@code PerSymbolMacdHistogramMagnitudeCalibrationTest} found for BTCUSDT. macd&gt;=0.75%
 * fails completely - worse than baseline at every checkpoint on the held-out tail
 * (-0.811%/-1.490%/-1.343%, n=17), a sharper reversal than either crypto symbol's own no-ship
 * outcome on this axis. AAPL keeps falling back to {@link
 * com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds#DEFAULT} (magnitude 0) on this
 * axis, same as it already does on the RSI axis per E8-F1-S7. See {@link
 * #sweepAaplOnItsOwnTuningWindow()}/{@link #validateAaplOnItsOwnHeldOutTail()}'s printed output
 * for the full figures and docs/CHANGELOG.md's E8-F1-S12 entry for the summarized numbers.
 *
 * <p>{@link #printSellSideAcrossCandidateGrid()} documents that unlike {@code rsiOverbought},
 * this axis gates the MACD vote symmetrically (per {@code
 * PerSymbolMacdHistogramMagnitudeCalibrationTest}'s own class Javadoc), so a hypothetical shipped
 * candidate here would necessarily also affect SELL-side classification - moot for this story
 * since nothing ships, but recorded for consistency with how every prior MACD-axis story treated
 * the SELL side. Printed only, no assertion beyond structural sanity, since there is no shipped
 * candidate to pin a SELL-side effect down against.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test - the printed
 * report is the evidence under review. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=StockPerSymbolMacdHistogramMagnitudeCalibrationTest}) for the actual
 * figures.
 */
class StockPerSymbolMacdHistogramMagnitudeCalibrationTest {

    /** Same grid every MACD-magnitude calibration test in this backlog has used - confirmed still
     * appropriately sized for AAPL by a throwaway probe (see class Javadoc). */
    private static final List<BigDecimal> CANDIDATE_MAGNITUDE_VALUES = List.of(
            new BigDecimal("0.00"), new BigDecimal("0.10"), new BigDecimal("0.25"), new BigDecimal("0.50"),
            new BigDecimal("0.75"), new BigDecimal("1.00"), new BigDecimal("1.50"), new BigDecimal("2.00"));

    private static final RuleThresholds DEFAULT = RuleThresholds.DEFAULT;

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void sweepAaplOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F1-S12: macdMinHistogramMagnitudePct swept for AAPL, TUNING WINDOW ONLY (first "
                + FixtureSplits.AAPL_SPLIT_INDEX + " candles) ##########");

        for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
            RuleThresholds candidate = thresholdsFor(magnitude);
            runAndPrint("AAPL [tuning]", FixtureSplits.AAPL_TUNING, candidateLabel(magnitude), candidate);
        }
    }

    @Test
    void validateAaplOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S12: every macdMinHistogramMagnitudePct candidate vs. AAPL's own held-out tail ##########");

        for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
            RuleThresholds candidate = thresholdsFor(magnitude);
            runAndPrint("AAPL [held-out tail]", FixtureSplits.AAPL_HELD_OUT, candidateLabel(magnitude), candidate);
        }
    }

    /** Not new evidence for a ship decision (nothing ships here) - a structural note, mirroring
     * {@code PerSymbolMacdHistogramMagnitudeCalibrationTest}'s own documented SELL-side coupling:
     * this axis gates {@code computeVotes}'s {@code macdBullish}/{@code macdBearish} reads
     * symmetrically, so it cannot be swept BUY-only at the vote-computation layer. Printed only,
     * since no candidate ships for AAPL and there is therefore no shipped SELL-side effect to
     * pin down as a real assertion the way {@code shippedSolusdtCandidateAlsoImprovesSellSide}
     * does for SOLUSDT. */
    @Test
    void printSellSideAcrossCandidateGrid() {
        System.out.println();
        System.out.println("########## E8-F1-S12: AAPL overallSell() across the same candidate grid (printed only, no ship here) ##########");
        for (List<Candle> window : List.of(FixtureSplits.AAPL_TUNING, FixtureSplits.AAPL_HELD_OUT)) {
            for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
                RuleThresholds candidate = thresholdsFor(magnitude);
                BacktestReport report = BacktestHarness.run("AAPL [sell check]", window, candidate);
                printCheckpointLine("  macd>=" + magnitude + "%", report.overallSell());
                assertStructurallySane(report);
            }
        }
    }

    private String candidateLabel(BigDecimal magnitude) {
        String suffix = magnitude.compareTo(BigDecimal.ZERO) == 0 ? " (current default, no filter)" : "";
        return "macd>=" + magnitude + "%" + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal magnitude) {
        return new RuleThresholds(DEFAULT.rsiOversold(), DEFAULT.rsiOverbought(), DEFAULT.volatilityExtreme(),
                DEFAULT.volumeDriedUp(), magnitude, DEFAULT.maMinSeparationPctOfPrice());
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

    /** Prints both raw and after-cost expectancy per checkpoint - this story's ship bar is decided
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
