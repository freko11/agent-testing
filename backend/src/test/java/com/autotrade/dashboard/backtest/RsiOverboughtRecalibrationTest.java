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
 * E8-F1-S3: recalibrates {@code rsiOverbought} independently of {@code rsiOversold} —
 * {@code RsiOversoldRecalibrationTest} (E8-F1-S2) ruled out {@code rsiOversold} as the lever
 * behind E8-F4-S1's still-open BUY-side out-of-sample mismatch (every candidate 24-32 produced
 * byte-identical BUY-side outcomes on every surface) and traced E8-F1-S1's original BUY-side
 * gain to the {@code rsiOverbought} move instead: a wider overbought band removes RSI-bearish
 * dissent votes on some bullish-leaning days, which is what let more BUY calls through. That
 * variable has never been isolated and tested on its own — this story closes that gap.
 *
 * <p>Two-phase design, identical to {@code RsiOversoldRecalibrationTest}'s corrected
 * tune-then-validate structure:
 * <ol>
 *   <li>{@link #sweepRsiOverboughtOnTuningWindowOnly()} sweeps rsiOverbought candidates against
 *   *only* the tuning window — {@code OutOfSampleValidationTest}'s own {@code SPLIT_INDEX = 700}
 *   first-700-candle slice of BTCUSDT/DOGEUSDT, reused here rather than inventing a second split
 *   boundary — never the held-out tail, never SOLUSDT. rsiOversold stays fixed at 25 throughout
 *   (E8-F1-S2 already showed it has zero effect on the BUY side and reverting it would only cost
 *   the already-working SELL side, so it is not re-litigated here), as do the two gate thresholds
 *   ({@link RuleThresholds#DEFAULT}'s volatilityExtreme/volumeDriedUp).</li>
 *   <li>{@link #validateCandidatesOutOfSample()} replays every grid candidate — not just
 *   whichever looked best on the tuning window — against the same three out-of-sample surfaces
 *   {@code OutOfSampleValidationTest} established: BTCUSDT held-out tail, DOGEUSDT held-out tail,
 *   and the genuinely untouched SOLUSDT fixture.</li>
 * </ol>
 *
 * <p>Candidate grid mirrors {@code RsiOversoldRecalibrationTest}'s shape: the full gap between
 * the current v2 value (75) and the pre-tuning v1 value (70) is swept one point at a time, plus
 * one boundary check one step beyond each end (76 above current, 68 below pre-tuning) — the same
 * "fill the gap, check one step past each known value" rationale, mirrored because overbought
 * moved 70&rarr;75 (up) where oversold moved 30&rarr;25 (down).
 *
 * <p><b>Ship bar (same bar {@code RsiOversoldRecalibrationTest} used):</b> a candidate only
 * replaces production {@code RSI_OVERBOUGHT_THRESHOLD} if its BUY-side
 * ({@code overallBuy().expectancyPctAfterCosts()}) is equal-or-better than the pre-tuning 30/70
 * baseline's at every one of MIN/MID/MAX, on <b>all three</b> out-of-sample surfaces, with a
 * comparably large scored {@code n} — the strict, all-surfaces bar, not a majority-of-surfaces
 * bar. The SELL side is checked too (same as E8-F1-S2's own report), since a BUY-side fix that
 * quietly breaks the already-working SELL side would not be a net improvement. See
 * docs/CHANGELOG.md's E8-F1-S3 entry for the actual result and decision.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review, not a regression target. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=RsiOverboughtRecalibrationTest}) for the actual figures.
 */
class RsiOverboughtRecalibrationTest {

    private static final List<Candle> SOLUSDT = FixtureSplits.SOLUSDT;

    /** Matches {@code OutOfSampleValidationTest}'s own tuning/held-out split exactly (now shared
     * via {@link FixtureSplits}, see that class's Javadoc). */
    private static final int SPLIT_INDEX = FixtureSplits.SPLIT_INDEX;

    private static final List<Candle> BTCUSDT_TUNING = FixtureSplits.BTCUSDT_TUNING;
    private static final List<Candle> DOGEUSDT_TUNING = FixtureSplits.DOGEUSDT_TUNING;
    private static final List<Candle> BTCUSDT_HELD_OUT = FixtureSplits.BTCUSDT_HELD_OUT;
    private static final List<Candle> DOGEUSDT_HELD_OUT = FixtureSplits.DOGEUSDT_HELD_OUT;

    private static final RuleThresholds DEFAULT = RuleThresholds.DEFAULT;

    /** rsiOversold fixed at 25 throughout — E8-F1-S2 already showed it has no measurable BUY-side
     * effect, not re-litigated here. 70 and 75 are carried as in-grid controls (already-known
     * values, now measured on the tuning-window slice specifically, which no prior test used).
     * 71-74 fill the gap between them; 68 is the one below-pre-tuning check, 76 the one
     * above-current check. */
    private static final List<BigDecimal> CANDIDATE_OVERBOUGHT_VALUES = List.of(
            new BigDecimal("68"), new BigDecimal("70"), new BigDecimal("71"), new BigDecimal("72"),
            new BigDecimal("73"), new BigDecimal("74"), new BigDecimal("75"), new BigDecimal("76"));

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void sweepRsiOverboughtOnTuningWindowOnly() {
        System.out.println();
        System.out.println("########## E8-F1-S3: rsiOverbought sweep, TUNING WINDOW ONLY (first " + SPLIT_INDEX + " candles) ##########");

        for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
            RuleThresholds candidate = thresholdsFor(overbought);
            String label = candidateLabel(overbought);

            BacktestReport btc = runAndPrint("BTCUSDT [tuning]", BTCUSDT_TUNING, label, candidate);
            BacktestReport doge = runAndPrint("DOGEUSDT [tuning]", DOGEUSDT_TUNING, label, candidate);

            DirectionalOutcomeStats combinedBuy = combineDirectional(btc.overallBuy(), doge.overallBuy());
            System.out.println();
            printCheckpointLine("  COMBINED BUY [" + label + "]", combinedBuy);
        }
    }

    @Test
    void validateCandidatesOutOfSample() {
        System.out.println();
        System.out.println("########## E8-F1-S3: every rsiOverbought candidate vs. held-out BTCUSDT/DOGEUSDT tails + untouched SOLUSDT ##########");

        for (BigDecimal overbought : CANDIDATE_OVERBOUGHT_VALUES) {
            RuleThresholds candidate = thresholdsFor(overbought);
            String label = candidateLabel(overbought);

            runAndPrint("BTCUSDT [held-out tail]", BTCUSDT_HELD_OUT, label, candidate);
            runAndPrint("DOGEUSDT [held-out tail]", DOGEUSDT_HELD_OUT, label, candidate);
            runAndPrint("SOLUSDT [untouched fixture]", SOLUSDT, label, candidate);
        }
    }

    private String candidateLabel(BigDecimal overbought) {
        String suffix = overbought.compareTo(new BigDecimal("75")) == 0 ? " (v2, current)"
                : overbought.compareTo(new BigDecimal("70")) == 0 ? " (pre-tuning)" : "";
        return "25/" + overbought + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal overbought) {
        return new RuleThresholds(SignalRuleEngine.RSI_OVERSOLD_THRESHOLD, overbought,
                DEFAULT.volatilityExtreme(), DEFAULT.volumeDriedUp(), DEFAULT.macdMinHistogramMagnitudePct());
    }

    private BacktestReport runAndPrint(String symbolLabel, List<Candle> candles, String thresholdLabel, RuleThresholds thresholds) {
        BacktestReport report = BacktestHarness.run(symbolLabel + " [" + thresholdLabel + "]", candles, thresholds);
        printCompact(report);
        assertStructurallySane(report);
        return report;
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

    private DirectionalOutcomeStats combineDirectional(DirectionalOutcomeStats a, DirectionalOutcomeStats b) {
        return new DirectionalOutcomeStats(a.totalCalls() + b.totalCalls(),
                BacktestHarness.combineCheckpoint(a.min(), b.min()),
                BacktestHarness.combineCheckpoint(a.mid(), b.mid()),
                BacktestHarness.combineCheckpoint(a.max(), b.max()));
    }

    /** Prints both raw and after-cost expectancy per checkpoint — this story's ship bar is decided
     * on {@code expectancyPctAfterCosts()} specifically, so it needs to be visible inline, not just
     * in a separate summary line. */
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
