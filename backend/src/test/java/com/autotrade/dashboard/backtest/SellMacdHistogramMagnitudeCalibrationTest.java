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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F1-S9: wires {@code macdMinHistogramMagnitudePct} in for SELL calls specifically, mirroring
 * how E8-F3-S3 wired the regime filter for SELL only — E8-F1-S5's original global sweep (and
 * E8-F1-S8's per-symbol BUY-side sweep, for SOLUSDT specifically) both flagged a consistent,
 * unactioned SELL-side improvement from this axis at nonzero candidates, chartered here as its own
 * story per those stories' own scope notes.
 *
 * <p>Unlike E8-F1-S8's per-symbol BUY-side mechanism, this story's AC calls for a single <b>global</b>
 * value wired for SELL classification across every symbol (mirroring {@code RegimeGatedRuleEngine
 * #applySellGate}'s crypto-wide, not per-symbol, scope) — so the ship bar is stricter: a candidate
 * must beat the {@code magnitude=0} baseline's SELL-side after-cost expectancy at every one of
 * MIN/MID/MAX on <b>all three</b> of BTCUSDT/DOGEUSDT/SOLUSDT's own tuning windows simultaneously,
 * then confirm on all three symbols' own held-out tails, before anything ships. A per-symbol winner
 * that doesn't generalize to the other two symbols does not clear this bar, even if it would have
 * cleared E8-F1-S4/S8's own per-symbol bar.
 *
 * <p><b>Actual result: no ship, and for the sharpest reason yet in this axis's history — BTCUSDT's
 * own tuning window never produces a single candidate that beats its magnitude=0 SELL-side baseline
 * (min/mid/max: -0.342%/-0.475%/-0.497%, n=172) at all three checkpoints simultaneously.</b> The
 * closest candidate (macd&gt;=0.10%) improves mid (-0.452%) and max (-0.453%) but is slightly worse
 * at min (-0.347% vs. -0.342%); every candidate at or above 0.25% is worse than the baseline at
 * every checkpoint. So there is no BTCUSDT tuning-window winner to even check against a held-out
 * tail, let alone one shared with the other two symbols — the global uniform-across-all-three-
 * symbols ship bar fails at the very first symbol checked, before DOGEUSDT/SOLUSDT's own results
 * are even relevant to the ship decision. {@link #noCandidateClearsTuningWindowBarOnAllThreeSymbolsAtOnce()}
 * pins this down as a real assertion (every candidate fails on at least one symbol's own tuning
 * window), not just a printed observation.
 *
 * <p>For completeness (not part of the ship decision, since the bar already failed on BTCUSDT):
 * DOGEUSDT and SOLUSDT each do have their own tuning-window winners that confirm on their own
 * held-out tails — DOGEUSDT at macd&gt;=0.75% (tuning min/mid/max +0.310%/+1.036%/+1.000%, n=57,
 * all beating its +0.175%/+0.616%/+0.521% baseline; held-out +0.731%/+2.249%/+2.593%, n=29, also
 * all beating its +0.230%/+1.042%/+1.206% baseline) and SOLUSDT across a wide range of candidates
 * from 0.10% through 1.50% (e.g. macd&gt;=0.10%, the same value E8-F1-S8 already shipped as
 * SOLUSDT's own per-symbol BUY-side override: tuning -0.399%/-0.073%/-0.072% vs. baseline
 * -0.528%/-0.216%/-0.216%, n=83; held-out +0.462%/+0.805%/+0.995% vs. baseline +0.357%/+0.647%/
 * +0.844%, n=45) — but DOGEUSDT's winner (0.75%) and SOLUSDT's own preferred range don't overlap
 * with any value that also works for BTCUSDT, because BTCUSDT has no winner at all. This is the
 * same asset-dependent, no-single-value-wins-everywhere conflict every other E8-F1 axis has hit,
 * now confirmed for the SELL side too, not just the BUY side E8-F1-S2/S3/S5/S6/S8 already
 * documented it for.
 *
 * <p>Net: nothing ships. No new {@code RuleThresholds} field, no new gate class, no {@link
 * SignalRuleEngine#RULE_TABLE_VERSION} bump, no {@code SignalService} change — this story's only
 * artifact is this calibration test itself, the same "ship only the investigation, not a value"
 * precedent E8-F1-S2/S3 set (rather than E8-F1-S5/S6/S8's precedent of also shipping an inert new
 * field, since {@code macdMinHistogramMagnitudePct} already exists from E8-F1-S5 and a SELL-only
 * gate mechanism would be genuinely new, evidence-gated code this finding doesn't justify writing).
 * E8-F1-S10/S11 (the MA-crossover-separation counterparts of E8-F1-S8/S9) remain open.
 */
class SellMacdHistogramMagnitudeCalibrationTest {

    /** Same grid every prior MACD-magnitude calibration test (E8-F1-S5, E8-F1-S8) used. */
    private static final List<BigDecimal> CANDIDATE_MAGNITUDE_VALUES = List.of(
            new BigDecimal("0.00"), new BigDecimal("0.10"), new BigDecimal("0.25"), new BigDecimal("0.50"),
            new BigDecimal("0.75"), new BigDecimal("1.00"), new BigDecimal("1.50"), new BigDecimal("2.00"));

    private static final BigDecimal BASELINE_MAGNITUDE = BigDecimal.ZERO;

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    @Test
    void sweepEachSymbolOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F1-S9: macdMinHistogramMagnitudePct swept per symbol, SELL SIDE, TUNING WINDOW ONLY ##########");
        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [tuning] ----");
            for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
                printSell(symbol.name() + " [tuning] macd>=" + magnitude + "%", symbol.tuning(), thresholdsFor(magnitude));
            }
        }
    }

    @Test
    void validateEachSymbolOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S9: every macdMinHistogramMagnitudePct candidate vs. that SAME symbol's own held-out tail, SELL SIDE ##########");
        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
                printSell(symbol.name() + " [held-out] macd>=" + magnitude + "%", symbol.heldOut(), thresholdsFor(magnitude));
            }
        }
    }

    /** Pins down the actual no-ship reason: for every swept candidate, at least one symbol's own
     * tuning-window SELL-side after-cost expectancy fails to beat the magnitude=0 baseline at all
     * three checkpoints simultaneously — so no value ever reaches the "uniform across all three
     * symbols" bar this story's global (not per-symbol) wiring requires, let alone the held-out
     * confirmation step after it. */
    @Test
    void noCandidateClearsTuningWindowBarOnAllThreeSymbolsAtOnce() {
        for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
            if (magnitude.compareTo(BASELINE_MAGNITUDE) == 0) {
                continue;
            }
            boolean clearsAllThree = SYMBOLS.stream()
                    .allMatch(symbol -> beatsBaselineAtEveryCheckpoint(symbol.tuning(), magnitude));
            assertFalse(clearsAllThree,
                    "macd>=" + magnitude + "% must not clear the tuning-window SELL-side bar on all three symbols "
                            + "at once (BTCUSDT never produces a qualifying candidate on its own tuning window)");
        }

        boolean btcusdtHasAnyTuningWindowWinner = CANDIDATE_MAGNITUDE_VALUES.stream()
                .filter(m -> m.compareTo(BASELINE_MAGNITUDE) != 0)
                .anyMatch(m -> beatsBaselineAtEveryCheckpoint(FixtureSplits.BTCUSDT_TUNING, m));
        assertFalse(btcusdtHasAnyTuningWindowWinner,
                "BTCUSDT's own SELL-side tuning window must have no candidate beating the magnitude=0 baseline "
                        + "at every checkpoint — the specific reason the global bar fails immediately");
    }

    private boolean beatsBaselineAtEveryCheckpoint(List<Candle> candles, BigDecimal magnitude) {
        DirectionalOutcomeStats baseline = BacktestHarness.run("baseline", candles, thresholdsFor(BASELINE_MAGNITUDE)).overallSell();
        DirectionalOutcomeStats candidate = BacktestHarness.run("candidate", candles, thresholdsFor(magnitude)).overallSell();
        return candidate.min().expectancyPctAfterCosts() > baseline.min().expectancyPctAfterCosts()
                && candidate.mid().expectancyPctAfterCosts() > baseline.mid().expectancyPctAfterCosts()
                && candidate.max().expectancyPctAfterCosts() > baseline.max().expectancyPctAfterCosts();
    }

    private RuleThresholds thresholdsFor(BigDecimal magnitude) {
        RuleThresholds d = RuleThresholds.DEFAULT;
        return new RuleThresholds(d.rsiOversold(), d.rsiOverbought(), d.volatilityExtreme(), d.volumeDriedUp(),
                magnitude, d.maMinSeparationPctOfPrice());
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
