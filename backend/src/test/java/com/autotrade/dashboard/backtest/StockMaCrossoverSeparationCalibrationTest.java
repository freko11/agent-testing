package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.MaCrossoverSellGate;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F1-S12: evaluates {@code maMinSeparationPctOfPrice} calibration against AAPL - this repo's
 * only stock fixture - on two distinct axes that were never checked against it:
 * <ol>
 *   <li>{@link #sweepAaplOnItsOwnTuningWindow()}/{@link #validateAaplOnItsOwnHeldOutTail()}: the
 *   same per-symbol BUY-side tune-then-confirm sweep {@code PerSymbolMaCrossoverSeparationCalibrationTest}
 *   (E8-F1-S10) ran per crypto symbol - AAPL gets a fresh, independent sweep here, never swept
 *   before on this axis.</li>
 *   <li>{@link #ma2PctAlreadyShippedGateValueVsAaplOwnBaseline()}: a narrower, distinct check of
 *   the already-shipped SELL-only gate value ({@link MaCrossoverSellGate#SELL_MIN_SEPARATION_PCT_OF_PRICE},
 *   2.00%) - does AAPL's own SELL-side after-cost expectancy at 2.00% separation beat its own
 *   {@code separation=0} baseline the way it did for all three crypto symbols in {@code
 *   SellMaCrossoverSeparationCalibrationTest} (E8-F1-S11)? A fixed-value check against
 *   already-shipped production behavior, not a fresh sweep.</li>
 * </ol>
 *
 * <p>A throwaway probe of {@code MovingAverageCrossoverCalculator} against AAPL's own tuning
 * window (median {@code separationPctOfPrice} 2.23%, p90 5.48%, max 9.45%) confirmed the existing
 * 0.00%-10.00% grid every crypto MA-crossover calibration test in this backlog has used remains
 * appropriately sized for AAPL - its span covers AAPL's own max with room past it - so it's
 * reused verbatim rather than re-derived. {@code rsiOversold}/{@code rsiOverbought} and {@code
 * macdMinHistogramMagnitudePct} stay fixed at the current global default (25/75/0) throughout,
 * mirroring {@code PerSymbolMaCrossoverSeparationCalibrationTest}'s own fixed-other-axes design.
 *
 * <p><b>Ship bar for #1 (per-symbol BUY-side override):</b> identical to {@code
 * PerSymbolMaCrossoverSeparationCalibrationTest}'s own per-symbol bar - AAPL gets a {@code
 * PerSymbolRuleThresholds} override only if some candidate's BUY-side ({@code
 * overallBuy().expectancyPctAfterCosts()}) beats the {@code separation=0} baseline at every one of
 * MIN/MID/MAX on AAPL's own tuning window, with a comparably large scored {@code n}, <i>and</i>
 * that same candidate still beats the baseline at every checkpoint on AAPL's own held-out tail.
 *
 * <p><b>Actual result for #1: no ship, but AAPL's tuning window does produce real winners this
 * time - unlike DOGEUSDT/SOLUSDT's own "no tuning-window winner to begin with" shape.</b> Four
 * candidates beat the {@code separation=0} baseline's BUY-side after-cost expectancy at all three
 * tuning-window checkpoints simultaneously: ma&gt;=1.00% (baseline min/mid/max
 * -0.256%/-0.036%/+0.216%, n=202 to -0.208%/+0.058%/+0.341%, n=173), ma&gt;=2.00% (to
 * -0.072%/+0.266%/+0.531%, n=128), ma&gt;=3.00% (to +0.064%/+0.165%/+0.334%, n=80), and
 * ma&gt;=4.00% (to +0.007%/+0.112%/+0.404%, n=55). But every one fails held-out confirmation, each
 * specifically at the MIN checkpoint (held-out baseline +0.111%/+0.189%/+0.279%, n=95): ma&gt;=1.00%
 * drops to +0.015%/+0.014%/+0.176% (worse at all three); ma&gt;=2.00% to +0.012%/+0.133%/+0.343%
 * (worse at MIN/MID, better at MAX); ma&gt;=3.00% to +0.056%/+0.355%/+0.448% (worse at MIN only);
 * ma&gt;=4.00% to -0.183%/-0.337%/-0.273% (worse at all three, most sharply). The MIN checkpoint is
 * the one that never confirms for any candidate - the same per-checkpoint failure mode {@code
 * PerSymbolMacdHistogramMagnitudeCalibrationTest} found for BTCUSDT on the MACD axis. AAPL keeps
 * falling back to {@link com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds#DEFAULT}
 * (separation 0) on the BUY side of this axis. See {@link #sweepAaplOnItsOwnTuningWindow()}/{@link
 * #validateAaplOnItsOwnHeldOutTail()}'s printed output for the full figures.
 *
 * <p><b>Actual result for #2: AAPL's own evidence actively contradicts the crypto-wide finding -
 * the shipped ma&gt;=2.00% value makes AAPL's SELL-side after-cost expectancy uniformly worse, not
 * better, at every checkpoint on both windows.</b> Tuning window: baseline
 * -0.537%/-0.955%/-1.173% (n=155) to ma&gt;=2.00% -0.613%/-1.355%/-1.590% (n=85) - worse at every
 * checkpoint. Held-out tail: baseline +0.333%/+1.163%/+1.401% (n=49) to ma&gt;=2.00%
 * +0.102%/+0.038%/-0.378% (n=28) - worse at every checkpoint there too, six checkpoints out of six
 * uniformly worse, not a mixed result. {@link #ma2PctAlreadyShippedGateValueVsAaplOwnBaseline()}
 * pins this contradiction down as a real assertion. This is the same shape of finding {@code
 * StockRegimeOutOfSampleValidationTest} (E8-F1-S7) already produced for the regime gate - a stock
 * that actively disagrees with the crypto-wide pattern its production mechanism already relies on,
 * not merely untested against it. {@link MaCrossoverSellGate#sellGateAppliesTo} stays crypto-only,
 * now with active negative stock evidence behind that scoping rather than merely absent evidence -
 * the same "gap closed with negative evidence, not left absent" treatment E8-F1-S7 gave {@code
 * PerSymbolRuleThresholds} and {@code RegimeGatedRuleEngine}'s own Javadocs. See
 * docs/CHANGELOG.md's E8-F1-S12 entry for the full figures.
 *
 * <p>Assertions here are structural only except {@link #ma2PctAlreadyShippedGateValueVsAaplOwnBaseline()},
 * which pins the #2 finding down as a real assertion rather than a printed observation. Read the
 * printed output (rerun via {@code ./mvnw test -Dtest=StockMaCrossoverSeparationCalibrationTest})
 * for the full figures.
 */
class StockMaCrossoverSeparationCalibrationTest {

    /** Same grid every MA-crossover-separation calibration test in this backlog has used -
     * confirmed still appropriately sized for AAPL by a throwaway probe (see class Javadoc). */
    private static final List<BigDecimal> CANDIDATE_SEPARATION_VALUES = List.of(
            new BigDecimal("0.00"), new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"),
            new BigDecimal("4.00"), new BigDecimal("5.00"), new BigDecimal("7.00"), new BigDecimal("10.00"));

    private static final BigDecimal BASELINE_SEPARATION = BigDecimal.ZERO;

    private static final RuleThresholds DEFAULT = RuleThresholds.DEFAULT;

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void sweepAaplOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F1-S12: maMinSeparationPctOfPrice swept for AAPL, TUNING WINDOW ONLY (first "
                + FixtureSplits.AAPL_SPLIT_INDEX + " candles) ##########");

        for (BigDecimal separation : CANDIDATE_SEPARATION_VALUES) {
            RuleThresholds candidate = thresholdsFor(separation);
            runAndPrint("AAPL [tuning]", FixtureSplits.AAPL_TUNING, candidateLabel(separation), candidate);
        }
    }

    @Test
    void validateAaplOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S12: every maMinSeparationPctOfPrice candidate vs. AAPL's own held-out tail ##########");

        for (BigDecimal separation : CANDIDATE_SEPARATION_VALUES) {
            RuleThresholds candidate = thresholdsFor(separation);
            runAndPrint("AAPL [held-out tail]", FixtureSplits.AAPL_HELD_OUT, candidateLabel(separation), candidate);
        }
    }

    /** Distinct, narrower check (see class Javadoc #2): does AAPL's own SELL-side after-cost
     * expectancy at the already-shipped {@link MaCrossoverSellGate#SELL_MIN_SEPARATION_PCT_OF_PRICE}
     * (2.00%) beat AAPL's own {@code separation=0} baseline, the way it did for all three crypto
     * symbols in {@code SellMaCrossoverSeparationCalibrationTest} (E8-F1-S11)? A fixed-value check
     * against already-shipped production behavior, not a fresh sweep.
     *
     * <p>Actual finding: no - the opposite. The shipped value makes AAPL's SELL-side after-cost
     * expectancy uniformly <em>worse</em> at every checkpoint on both the tuning window and the
     * held-out tail (six checkpoints out of six), a clean contradiction of the crypto-wide pattern,
     * not a mixed or marginal result. Pinned down below as a real assertion in the direction
     * actually found, plus a scoping assertion confirming {@link MaCrossoverSellGate#sellGateAppliesTo}
     * still excludes stocks. */
    @Test
    void ma2PctAlreadyShippedGateValueVsAaplOwnBaseline() {
        BigDecimal shipped = MaCrossoverSellGate.SELL_MIN_SEPARATION_PCT_OF_PRICE;

        DirectionalOutcomeStats tuningBaseline = BacktestHarness.run("AAPL [tuning baseline]",
                FixtureSplits.AAPL_TUNING, thresholdsFor(BASELINE_SEPARATION)).overallSell();
        DirectionalOutcomeStats tuningShipped = BacktestHarness.run("AAPL [tuning shipped-value]",
                FixtureSplits.AAPL_TUNING, thresholdsFor(shipped)).overallSell();
        DirectionalOutcomeStats heldOutBaseline = BacktestHarness.run("AAPL [held-out baseline]",
                FixtureSplits.AAPL_HELD_OUT, thresholdsFor(BASELINE_SEPARATION)).overallSell();
        DirectionalOutcomeStats heldOutShipped = BacktestHarness.run("AAPL [held-out shipped-value]",
                FixtureSplits.AAPL_HELD_OUT, thresholdsFor(shipped)).overallSell();

        System.out.println();
        System.out.println("########## E8-F1-S12: AAPL SELL-side, separation=0 baseline vs. already-shipped ma>=2.00% ##########");
        printCheckpointLine("  [tuning]    baseline (ma>=0.00%)", tuningBaseline);
        printCheckpointLine("  [tuning]    shipped  (ma>=" + shipped + "%)", tuningShipped);
        printCheckpointLine("  [held-out]  baseline (ma>=0.00%)", heldOutBaseline);
        printCheckpointLine("  [held-out]  shipped  (ma>=" + shipped + "%)", heldOutShipped);

        // Pins down the actual (negative) finding: the shipped value is worse, not better, at
        // every checkpoint on both windows - a clean contradiction of the crypto-wide E8-F1-S11
        // finding, not a mixed result.
        assertContradictsBaselineAtEveryCheckpoint(tuningBaseline, tuningShipped, "AAPL tuning");
        assertContradictsBaselineAtEveryCheckpoint(heldOutBaseline, heldOutShipped, "AAPL held-out");

        // Scoping check: a stock symbol that actively contradicts the crypto-wide finding must not
        // widen MaCrossoverSellGate's crypto-only scoping - confirms the production code agrees.
        assertFalse(MaCrossoverSellGate.sellGateAppliesTo(AssetType.STOCK),
                "AAPL: contradicting stock evidence must not widen MaCrossoverSellGate's crypto-only scoping");
    }

    private void assertContradictsBaselineAtEveryCheckpoint(DirectionalOutcomeStats baseline, DirectionalOutcomeStats shipped, String label) {
        assertTrue(shipped.min().expectancyPctAfterCosts() < baseline.min().expectancyPctAfterCosts(),
                label + ": already-shipped ma>=2.00% is expected to make AAPL's SELL-side min expectancy worse (documented contradiction)");
        assertTrue(shipped.mid().expectancyPctAfterCosts() < baseline.mid().expectancyPctAfterCosts(),
                label + ": already-shipped ma>=2.00% is expected to make AAPL's SELL-side mid expectancy worse (documented contradiction)");
        assertTrue(shipped.max().expectancyPctAfterCosts() < baseline.max().expectancyPctAfterCosts(),
                label + ": already-shipped ma>=2.00% is expected to make AAPL's SELL-side max expectancy worse (documented contradiction)");
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
