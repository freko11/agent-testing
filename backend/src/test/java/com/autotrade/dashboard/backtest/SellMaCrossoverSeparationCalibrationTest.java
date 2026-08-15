package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.MaCrossoverSellGate;
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
 * E8-F1-S11: wires {@code maMinSeparationPctOfPrice} in for SELL calls specifically, mirroring how
 * E8-F1-S9 attempted the same SELL-only wiring for {@code macdMinHistogramMagnitudePct} — E8-F1-S6's
 * original global sweep flagged a ~2.00% separation threshold as improving SELL-side after-cost
 * expectancy uniformly across all three symbols (BTCUSDT/DOGEUSDT/SOLUSDT) on their own held-out
 * tails, but that story was chartered for the BUY-side mismatch and left the finding unactioned,
 * chartered here as its own story per that story's own scope note.
 *
 * <p>Same global (not per-symbol) ship bar E8-F1-S9 used, mirroring {@code RegimeGatedRuleEngine
 * #applySellGate}'s crypto-wide, not per-symbol, scope: a candidate must beat the {@code
 * separation=0} baseline's SELL-side after-cost expectancy at every one of MIN/MID/MAX on <b>all
 * three</b> of BTCUSDT/DOGEUSDT/SOLUSDT's own tuning windows simultaneously, before even reaching
 * held-out confirmation. A per-symbol winner that doesn't generalize to the other two symbols does
 * not clear this bar, even if it would have cleared E8-F1-S8/S10's own per-symbol bar.
 *
 * <p><b>Actual result: ship, unlike E8-F1-S9's own attempt on the MACD axis.</b> {@code
 * ma&gt;=2.00%} clears the tuning-window bar on all three symbols simultaneously — BTCUSDT (tuning
 * min/mid/max baseline -0.342%/-0.475%/-0.497%, n=172 &rarr; ma&gt;=2.00% -0.181%/-0.428%/-0.424%,
 * n=115, all three better), DOGEUSDT (baseline +0.175%/+0.616%/+0.521%, n=94 &rarr; ma&gt;=2.00%
 * +0.207%/+0.844%/+0.787%, n=77, all three better), and SOLUSDT (baseline -0.528%/-0.216%/-0.216%,
 * n=87 &rarr; ma&gt;=2.00% -0.414%/-0.134%/-0.133%, n=69, all three better) — the same value
 * E8-F1-S6/S10's own secondary findings had already flagged. It then confirms on all three
 * symbols' own held-out tails too: BTCUSDT (baseline +0.736%/+0.963%/+0.999%, n=67 &rarr;
 * ma&gt;=2.00% +1.046%/+1.191%/+1.213%, n=52, all three better), DOGEUSDT (baseline
 * +0.230%/+1.042%/+1.206%, n=48 &rarr; ma&gt;=2.00% +0.504%/+1.265%/+1.612%, n=36, all three
 * better), and SOLUSDT (baseline +0.357%/+0.647%/+0.844%, n=49 &rarr; ma&gt;=2.00%
 * +1.002%/+1.246%/+1.506%, n=37, all three better). {@link #tuningWindowBarOutcomeAcrossAllThreeSymbols()}
 * pins the tuning-window clearance down as a real assertion outcome (prints {@code true}), and
 * {@code MaCrossoverSellGate}'s own class Javadoc records the wiring decision this evidence
 * supports. Shipped: {@code MaCrossoverSellGate.applySellGate} wired into {@code
 * SignalService.computeSignalWithProvenance} for crypto tickers, {@link
 * SignalRuleEngine#RULE_TABLE_VERSION} bumps v5&rarr;v6. See docs/CHANGELOG.md's E8-F1-S11 entry
 * for the full figures and the recomputed {@code LiveDriftBaseline} SELL constants.
 */
class SellMaCrossoverSeparationCalibrationTest {

    /** Same grid {@code MaCrossoverSeparationCalibrationTest} (E8-F1-S6) and {@code
     * PerSymbolMaCrossoverSeparationCalibrationTest} (E8-F1-S10) used. */
    private static final List<BigDecimal> CANDIDATE_SEPARATION_VALUES = List.of(
            new BigDecimal("0.00"), new BigDecimal("1.00"), new BigDecimal("2.00"), new BigDecimal("3.00"),
            new BigDecimal("4.00"), new BigDecimal("5.00"), new BigDecimal("7.00"), new BigDecimal("10.00"));

    private static final BigDecimal BASELINE_SEPARATION = BigDecimal.ZERO;

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    @Test
    void sweepEachSymbolOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F1-S11: maMinSeparationPctOfPrice swept per symbol, SELL SIDE, TUNING WINDOW ONLY ##########");
        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [tuning] ----");
            for (BigDecimal separation : CANDIDATE_SEPARATION_VALUES) {
                printSell(symbol.name() + " [tuning] ma>=" + separation + "%", symbol.tuning(), thresholdsFor(separation));
            }
        }
    }

    @Test
    void validateEachSymbolOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S11: every maMinSeparationPctOfPrice candidate vs. that SAME symbol's own held-out tail, SELL SIDE ##########");
        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal separation : CANDIDATE_SEPARATION_VALUES) {
                printSell(symbol.name() + " [held-out] ma>=" + separation + "%", symbol.heldOut(), thresholdsFor(separation));
            }
        }
    }

    /** Pins down the actual ship finding: {@code ma>=2.00%} clears the "uniform across all three
     * symbols" tuning-window bar this story's global (not per-symbol) wiring requires, and then
     * also confirms on all three symbols' own held-out tails — see class Javadoc for the full
     * per-symbol figures and {@link com.autotrade.dashboard.signal.MaCrossoverSellGate} for the
     * production wiring this finding supports. */
    @Test
    void ma2PctClearsTuningWindowBarOnAllThreeSymbolsAndConfirmsOnHeldOut() {
        BigDecimal shipped = MaCrossoverSellGate.SELL_MIN_SEPARATION_PCT_OF_PRICE;

        for (SymbolFixture symbol : SYMBOLS) {
            assertTrue(beatsBaselineAtEveryCheckpoint(symbol.tuning(), shipped),
                    symbol.name() + ": ma>=" + shipped + "% must beat the separation=0 SELL-side baseline "
                            + "at every checkpoint on its own tuning window");
            assertTrue(beatsBaselineAtEveryCheckpoint(symbol.heldOut(), shipped),
                    symbol.name() + ": ma>=" + shipped + "% must also beat the separation=0 SELL-side baseline "
                            + "at every checkpoint on its own held-out tail");
        }
    }

    private boolean beatsBaselineAtEveryCheckpoint(List<Candle> candles, BigDecimal separation) {
        DirectionalOutcomeStats baseline = BacktestHarness.run("baseline", candles, thresholdsFor(BASELINE_SEPARATION)).overallSell();
        DirectionalOutcomeStats candidate = BacktestHarness.run("candidate", candles, thresholdsFor(separation)).overallSell();
        return candidate.min().expectancyPctAfterCosts() > baseline.min().expectancyPctAfterCosts()
                && candidate.mid().expectancyPctAfterCosts() > baseline.mid().expectancyPctAfterCosts()
                && candidate.max().expectancyPctAfterCosts() > baseline.max().expectancyPctAfterCosts();
    }

    private RuleThresholds thresholdsFor(BigDecimal separation) {
        RuleThresholds d = RuleThresholds.DEFAULT;
        return new RuleThresholds(d.rsiOversold(), d.rsiOverbought(), d.volatilityExtreme(), d.volumeDriedUp(),
                d.macdMinHistogramMagnitudePct(), separation);
    }

    private void printSell(String label, List<Candle> candles, RuleThresholds thresholds) {
        BacktestReport report = BacktestHarness.run(label, candles, thresholds);
        assertStructurallySane(report);
        DirectionalOutcomeStats sell = report.overallSell();
        if (sell.totalCalls() == 0) {
            System.out.printf("%-40s (n=0)%n", label);
            return;
        }
        System.out.printf("%-40s min %5.1f%%win exp%+7.3f%%(aft%+7.3f%%)(n=%-3d) | mid %5.1f%%win exp%+7.3f%%(aft%+7.3f%%)(n=%-3d) | max %5.1f%%win exp%+7.3f%%(aft%+7.3f%%)(n=%-3d)%n",
                label,
                sell.min().winRate(), sell.min().expectancyPct(), sell.min().expectancyPctAfterCosts(), sell.min().scored(),
                sell.mid().winRate(), sell.mid().expectancyPct(), sell.mid().expectancyPctAfterCosts(), sell.mid().scored(),
                sell.max().winRate(), sell.max().expectancyPct(), sell.max().expectancyPctAfterCosts(), sell.max().scored());
    }

    /** Same structural invariants every other E8 calibration test checks. */
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
