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
 * E8-F1-S8: calibrates {@code macdMinHistogramMagnitudePct} <b>per ticker symbol</b> instead of as
 * one global value — the mechanism E8-F1-S6's own closing note named as the one still-untried
 * lever after RSI bounds (E8-F1-S2/S3) and this exact MACD axis's own earlier global sweep
 * ({@code MacdHistogramMagnitudeCalibrationTest}, E8-F1-S5) each hit the same asset-divergent,
 * no-single-value-wins-everywhere conflict on the BUY side (BTCUSDT improves at MID/MAX but not
 * MIN, SOLUSDT improves near 0.50%-1.00%, DOGEUSDT prefers no filter at all).
 *
 * <p>Same independent-per-symbol tune/held-out design {@code PerSymbolRsiOverboughtCalibrationTest}
 * (E8-F1-S4) established, applied to this axis the same way {@code MacdHistogramMagnitudeCalibrationTest}
 * applied the (pooled, all-three-simultaneous) global bar to it:
 * <ol>
 *   <li>{@link #sweepEachSymbolOnItsOwnTuningWindow()} sweeps the same 8-candidate grid
 *   {@code MacdHistogramMagnitudeCalibrationTest} used (0.00% through 2.00%) against each of
 *   BTCUSDT/DOGEUSDT/SOLUSDT's own first {@link FixtureSplits#SPLIT_INDEX} candles only — never
 *   that symbol's own held-out tail, never another symbol's data. {@code rsiOversold}/{@code
 *   rsiOverbought} stay fixed at the current global default (25/75) throughout, as does {@code
 *   maMinSeparationPctOfPrice} (0).</li>
 *   <li>{@link #validateEachSymbolOnItsOwnHeldOutTail()} replays every candidate for a symbol
 *   against that <b>same</b> symbol's own held-out tail (candles 700-1000) — never a different
 *   symbol's tail.</li>
 * </ol>
 *
 * <p><b>Ship bar, per symbol independently (E8-F1-S4's bar, not {@code
 * MacdHistogramMagnitudeCalibrationTest}'s all-three-simultaneous one — that difference is exactly
 * why the earlier global sweep no-shipped everywhere):</b> a symbol gets a {@code
 * PerSymbolRuleThresholds} override only if (a) some candidate's BUY-side ({@code
 * overallBuy().expectancyPctAfterCosts()}) beats the {@code magnitude=0} baseline at every one of
 * MIN/MID/MAX on that symbol's own tuning window, with a comparably large scored {@code n}, and
 * (b) that same candidate's BUY-side still beats the {@code magnitude=0} baseline at every
 * checkpoint on that symbol's own held-out tail. A symbol whose tuning window produces no such
 * winner, or whose winner doesn't confirm on its own held-out tail, ships no override and falls
 * back to {@link RuleThresholds#DEFAULT} (magnitude 0) — a legitimate per-symbol no-ship, same
 * treatment E8-F1-S4 gave BTCUSDT/DOGEUSDT's own no-ship outcome on the RSI axis.
 *
 * <p>{@code overallSell()} is printed for every candidate (see {@link #printCompact}) but never
 * gates the ship decision here — acting on a SELL-side effect is E8-F1-S9's separate, chartered
 * story. Unlike {@code rsiOverbought} (which E8-F1-S3 found has zero measurable SELL-side effect
 * anywhere in its swept range), {@code macdMinHistogramMagnitudePct} gates {@code computeVotes}'s
 * {@code macdBullish}/{@code macdBearish} reads symmetrically off the same threshold — there is no
 * way to make this BUY-only at the vote-computation layer without an out-of-scope {@code evaluate}
 * signature change, so whatever candidate a symbol actually ships (if any) necessarily also changes
 * that symbol's SELL-side classification. See docs/CHANGELOG.md's E8-F1-S8 entry and {@link
 * com.autotrade.dashboard.signal.PerSymbolRuleThresholds}'s own class Javadoc for the actual
 * per-symbol result, including the documented SELL-side effect of whatever shipped.
 *
 * <p><b>Actual result of the real run:</b> BTCUSDT and DOGEUSDT both ship no override; SOLUSDT
 * does. BTCUSDT's own tuning-window winners (macd&gt;=0.75%, n=64, and macd&gt;=1.00%, n=39 — both
 * beat the magnitude-0 baseline's after-cost expectancy at every checkpoint) both fail held-out
 * confirmation at the MIN checkpoint specifically (0.75%: held-out min -0.488% vs. baseline
 * -0.425%; 1.00%: held-out min -0.429% vs. baseline -0.425%) even though both do confirm at MID
 * and MAX. DOGEUSDT's only tuning-window winner (macd&gt;=0.10%, n=127 vs. baseline n=132) fails
 * held-out confirmation completely — worse than the baseline at every one of MIN/MID/MAX on its
 * own held-out tail (e.g. max +1.164% vs. baseline's +1.226%), not just a partial miss like
 * BTCUSDT's. SOLUSDT's winner (macd&gt;=0.10%, the same value DOGEUSDT's tuning window also
 * favored but couldn't confirm) is a genuine, non-degenerate confirmation: it beats the
 * magnitude-0 baseline's BUY-side after-cost expectancy at every checkpoint on <em>both</em>
 * windows, with comparable {@code n} throughout (tuning 186 vs. 188, held-out 67 vs. 69) — see
 * {@link #sweepEachSymbolOnItsOwnTuningWindow()}/{@link #validateEachSymbolOnItsOwnHeldOutTail()}'s
 * printed output for the full figures.
 *
 * <p><b>SOLUSDT's shipped candidate's SELL-side effect (checked and pinned down, per this story's
 * AC, since this axis cannot be made BUY-only at the vote-computation layer):</b> macd&gt;=0.10%
 * also improves SOLUSDT's own {@code overallSell()} after-cost expectancy at every checkpoint, on
 * both the tuning window (baseline min/mid/max -0.528%/-0.216%/-0.216%, n=87 &rarr; shipped
 * -0.399%/-0.073%/-0.072%, n=83) and the held-out tail (baseline +0.357%/+0.647%/+0.844%, n=49
 * &rarr; shipped +0.462%/+0.805%/+0.995%, n=45) — a real, positive SELL-side effect, not a neutral
 * one, matching the direction {@code MacdHistogramMagnitudeCalibrationTest} (E8-F1-S5) already
 * flagged as a consistent-but-unactioned secondary finding across all three symbols at nonzero
 * candidates. {@link #shippedSolusdtCandidateAlsoImprovesSellSide()} pins this down as a real
 * assertion, not just a printed observation. Acting on it beyond SOLUSDT (e.g. wiring a SELL-only
 * gate) is E8-F1-S9's separate, chartered story.
 *
 * <p>Assertions here are structural only (plus the one pinned SOLUSDT SELL-side effect above),
 * mirroring every other E8 calibration test — the printed report is the evidence the ship decision
 * above was actually made from. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=PerSymbolMacdHistogramMagnitudeCalibrationTest}) for the full figures.
 */
class PerSymbolMacdHistogramMagnitudeCalibrationTest {

    /** Same grid {@code MacdHistogramMagnitudeCalibrationTest} (E8-F1-S5) swept globally, reused
     * verbatim rather than re-deriving a new grid per symbol. */
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
        System.out.println("########## E8-F1-S8: macdMinHistogramMagnitudePct swept per symbol, TUNING WINDOW ONLY (first "
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
        System.out.println("########## E8-F1-S8: every macdMinHistogramMagnitudePct candidate vs. that SAME symbol's own held-out tail ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal magnitude : CANDIDATE_MAGNITUDE_VALUES) {
                RuleThresholds candidate = thresholdsFor(magnitude);
                runAndPrint(symbol.name() + " [held-out tail]", symbol.heldOut(), candidateLabel(magnitude), candidate);
            }
        }
    }

    /** Not new BUY-side evidence (see class Javadoc) — pins down, structurally, what SOLUSDT's
     * shipped candidate (macd&gt;=0.10%) actually does to its own SELL side. Unlike {@code
     * rsiOverbought} (which E8-F1-S3 found has zero measurable SELL-side effect anywhere in its
     * swept range), {@code macdMinHistogramMagnitudePct} gates {@code computeVotes}'s {@code
     * macdBullish}/{@code macdBearish} reads symmetrically off the same threshold, so shipping a
     * BUY-side-confirmed candidate necessarily also changes SELL-side classification. Finding:
     * the effect is real and positive on both windows, not neutral or negative. */
    @Test
    void shippedSolusdtCandidateAlsoImprovesSellSide() {
        RuleThresholds shipped = thresholdsFor(new BigDecimal("0.10"));

        DirectionalOutcomeStats tuningBaseline = BacktestHarness.run("SOLUSDT [tuning baseline]",
                FixtureSplits.SOLUSDT_TUNING, DEFAULT).overallSell();
        DirectionalOutcomeStats tuningShipped = BacktestHarness.run("SOLUSDT [tuning shipped]",
                FixtureSplits.SOLUSDT_TUNING, shipped).overallSell();
        assertSellSideImproves(tuningBaseline, tuningShipped, "SOLUSDT tuning");

        DirectionalOutcomeStats heldOutBaseline = BacktestHarness.run("SOLUSDT [held-out baseline]",
                FixtureSplits.SOLUSDT_HELD_OUT, DEFAULT).overallSell();
        DirectionalOutcomeStats heldOutShipped = BacktestHarness.run("SOLUSDT [held-out shipped]",
                FixtureSplits.SOLUSDT_HELD_OUT, shipped).overallSell();
        assertSellSideImproves(heldOutBaseline, heldOutShipped, "SOLUSDT held-out");
    }

    private void assertSellSideImproves(DirectionalOutcomeStats baseline, DirectionalOutcomeStats shipped, String label) {
        assertTrue(shipped.min().expectancyPctAfterCosts() > baseline.min().expectancyPctAfterCosts(),
                label + ": shipped candidate (macd>=0.10%) must improve SELL-side min expectancy vs. baseline (documented side effect)");
        assertTrue(shipped.mid().expectancyPctAfterCosts() > baseline.mid().expectancyPctAfterCosts(),
                label + ": shipped candidate (macd>=0.10%) must improve SELL-side mid expectancy vs. baseline (documented side effect)");
        assertTrue(shipped.max().expectancyPctAfterCosts() > baseline.max().expectancyPctAfterCosts(),
                label + ": shipped candidate (macd>=0.10%) must improve SELL-side max expectancy vs. baseline (documented side effect)");
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
