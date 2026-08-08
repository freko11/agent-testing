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
 * E8-F1-S4: calibrates {@code rsiOverbought} <b>per ticker symbol</b> instead of as one global
 * value — the fix E8-F1-S3 flagged as untried after finding the BUY-side optimum genuinely
 * diverges by asset in the 68-76 grid (BTCUSDT wants it low, DOGEUSDT wants it high, SOLUSDT is
 * best near the pre-tuning 70) with no single value beating the pre-tuning baseline on all three
 * surfaces at once.
 *
 * <p>Unlike every prior E8-F1 calibration test, this one runs the tune-then-validate cycle
 * <b>independently per symbol</b> rather than pooling BTCUSDT/DOGEUSDT into one tuning decision
 * and using SOLUSDT only as a held-out check:
 * <ol>
 *   <li>{@link #sweepEachSymbolOnItsOwnTuningWindow()} sweeps the same 68-76 candidate grid
 *   {@code RsiOverboughtRecalibrationTest} used, against each of BTCUSDT/DOGEUSDT/SOLUSDT's own
 *   first {@link FixtureSplits#SPLIT_INDEX} candles only — never that symbol's own held-out tail,
 *   never another symbol's data. rsiOversold stays fixed at 25 throughout (E8-F1-S2's finding: no
 *   measurable BUY-side effect), as do the two gate thresholds ({@link RuleThresholds#DEFAULT}'s
 *   volatilityExtreme/volumeDriedUp).</li>
 *   <li>{@link #validateEachSymbolOnItsOwnHeldOutTail()} replays every candidate for a symbol
 *   against that <b>same</b> symbol's own held-out tail (candles 700-1000) — never a different
 *   symbol's tail, never SOLUSDT used as a stand-in "third opinion" for BTCUSDT or DOGEUSDT. This
 *   is a within-symbol chronological generalization check, a different and correctly-scoped
 *   question from {@code OutOfSampleValidationTest}'s cross-symbol check.</li>
 * </ol>
 *
 * <p><b>Ship bar, per symbol independently:</b> a symbol gets a {@code PerSymbolRuleThresholds}
 * override only if (a) some candidate's BUY-side ({@code overallBuy().expectancyPctAfterCosts()})
 * beats the current global default (75) at every one of MIN/MID/MAX on that symbol's own tuning
 * window, with a comparably large scored {@code n}, <i>and</i> (b) that same candidate's BUY-side
 * still beats 75 at every checkpoint on that symbol's own held-out tail. A symbol whose tuning
 * window produces no such winner, or whose winner doesn't confirm on its own held-out tail, ships
 * no override and falls back to {@link RuleThresholds#DEFAULT} — a legitimate per-symbol no-ship,
 * same treatment E8-F1-S2/S3 gave their own global no-ship outcomes. See docs/CHANGELOG.md's
 * E8-F1-S4 entry for the actual per-symbol figures and decisions.
 *
 * <p>{@link #sellSideUnaffectedByOverboughtCandidates()} is a structural sanity check (not new
 * evidence): E8-F1-S3 already established that {@code rsiOverbought} has zero measurable effect
 * on SELL-side classification anywhere in the swept range. This asserts that isolation held here
 * too — every candidate's {@code overallSell()} is byte-identical to the {@code RuleThresholds
 * .DEFAULT} run, for every symbol and every window — rather than re-deriving it as a new finding.
 *
 * <p><b>Fixture-exhaustion caveat:</b> this is the first E8 story to tune against SOLUSDT's own
 * data (its prior role, in every story since E8-F4-S1, was strictly as a fully "untouched"
 * out-of-sample check). After this story ships, no fixture among BTCUSDT/DOGEUSDT/SOLUSDT remains
 * genuinely untouched for a future recalibration story to validate against — a real constraint on
 * what evidence a next threshold change could offer, confirmed with the user as an accepted
 * tradeoff before implementation rather than a blocker, per this story's design gate.
 *
 * <p>Assertions here are structural (plus the SELL-side byte-identical check above), mirroring
 * every other E8 calibration test — the printed report is the evidence the ship decision above was
 * actually made from. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=PerSymbolRsiOverboughtCalibrationTest}) for the actual figures.
 */
class PerSymbolRsiOverboughtCalibrationTest {

    /** Same grid {@code RsiOverboughtRecalibrationTest} (E8-F1-S3) swept: the gap between the
     * pre-tuning 70 and current 75 filled one point at a time, plus one boundary check past each
     * end. Reused verbatim rather than re-deriving a new grid per symbol. */
    private static final List<BigDecimal> CANDIDATE_OVERBOUGHT_VALUES = List.of(
            new BigDecimal("68"), new BigDecimal("70"), new BigDecimal("71"), new BigDecimal("72"),
            new BigDecimal("73"), new BigDecimal("74"), new BigDecimal("75"), new BigDecimal("76"));

    private static final BigDecimal CURRENT_DEFAULT_OVERBOUGHT = SignalRuleEngine.RSI_OVERBOUGHT_THRESHOLD;

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
        System.out.println("########## E8-F1-S4: rsiOverbought swept per symbol, TUNING WINDOW ONLY (first "
                + FixtureSplits.SPLIT_INDEX + " candles each) ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [tuning] ----");
            for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
                RuleThresholds candidate = thresholdsFor(overbought);
                runAndPrint(symbol.name() + " [tuning]", symbol.tuning(), candidateLabel(overbought), candidate);
            }
        }
    }

    @Test
    void validateEachSymbolOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F1-S4: every rsiOverbought candidate vs. that SAME symbol's own held-out tail ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
                RuleThresholds candidate = thresholdsFor(overbought);
                runAndPrint(symbol.name() + " [held-out tail]", symbol.heldOut(), candidateLabel(overbought), candidate);
            }
        }
    }

    /** Structural sanity check, not new evidence (see class Javadoc): confirms
     * {@code rsiOverbought} moves nothing on the SELL side, for any symbol/window, matching
     * E8-F1-S3's own finding. */
    @Test
    void sellSideUnaffectedByOverboughtCandidates() {
        for (SymbolFixture symbol : SYMBOLS) {
            for (List<Candle> window : List.of(symbol.tuning(), symbol.heldOut())) {
                DirectionalOutcomeStats baselineSell = BacktestHarness.run(symbol.name(), window, DEFAULT).overallSell();
                for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
                    RuleThresholds candidate = thresholdsFor(overbought);
                    DirectionalOutcomeStats candidateSell = BacktestHarness.run(symbol.name(), window, candidate).overallSell();
                    assertEquals(baselineSell, candidateSell, symbol.name()
                            + ": overallSell() must be byte-identical to DEFAULT regardless of rsiOverbought (candidate " + overbought + ")");
                }
            }
        }
    }

    private String candidateLabel(BigDecimal overbought) {
        String suffix = overbought.compareTo(CURRENT_DEFAULT_OVERBOUGHT) == 0 ? " (v2/current default)"
                : overbought.compareTo(new BigDecimal("70")) == 0 ? " (pre-tuning)" : "";
        return "25/" + overbought + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal overbought) {
        return new RuleThresholds(SignalRuleEngine.RSI_OVERSOLD_THRESHOLD, overbought,
                DEFAULT.volatilityExtreme(), DEFAULT.volumeDriedUp());
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
