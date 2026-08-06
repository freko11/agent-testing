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
 * E8-F1-S2: recalibrates {@code rsiOversold} independently of {@code rsiOverbought} — the
 * follow-up {@code OutOfSampleValidationTest} (E8-F4-S1) flagged when it found E8-F1-S1's
 * symmetric RSI 25/75 shift replicates out-of-sample on the SELL side but not the BUY side (see
 * docs/CHANGELOG.md's E8-F4-S1 entry for the exact figures).
 *
 * <p>Two-phase design, deliberately correcting E8-F1-S1's original tune-on-full-fixture,
 * test-on-the-same-fixture mistake:
 * <ol>
 *   <li>{@link #sweepRsiOversoldOnTuningWindowOnly()} sweeps rsiOversold candidates against
 *   *only* the tuning window — {@code OutOfSampleValidationTest}'s own {@code SPLIT_INDEX = 700}
 *   first-700-candle slice of BTCUSDT/DOGEUSDT, reused here rather than inventing a second split
 *   boundary — never the held-out tail, never SOLUSDT. rsiOverbought stays fixed at 75 throughout
 *   (already OOS-validated by E8-F4-S1, not re-litigated here), as do the two gate thresholds
 *   ({@link RuleThresholds#DEFAULT}'s volatilityExtreme/volumeDriedUp).</li>
 *   <li>{@link #validateCandidatesOutOfSample()} replays every grid candidate — not just
 *   whichever looked best on the tuning window, since that ranking alone is exactly what this
 *   story doesn't trust — against the same three out-of-sample surfaces
 *   {@code OutOfSampleValidationTest} established: BTCUSDT held-out tail, DOGEUSDT held-out tail,
 *   and the genuinely untouched SOLUSDT fixture.</li>
 * </ol>
 *
 * <p><b>Ship bar (confirmed with the user before this sweep was run):</b> a candidate only
 * replaces production {@code RSI_OVERSOLD_THRESHOLD} if its BUY-side
 * ({@code overallBuy().expectancyPctAfterCosts()}) is equal-or-better than the pre-tuning 30/70
 * baseline's at every one of MIN/MID/MAX, on <b>all three</b> out-of-sample surfaces, with a
 * comparably large scored {@code n} (not winning by filtering down to a smaller, noisier subset)
 * — the strict, all-surfaces bar, not a majority-of-surfaces bar, matching
 * {@code RegimeGatedRuleEngine}'s (E8-F3-S2) precedent for treating anything less than uniform
 * evidence as no-ship. This is deliberately benchmarked against 30, not 25 — 25 already failed
 * this exact bar in E8-F4-S1. See docs/CHANGELOG.md's E8-F1-S2 entry for the actual result and
 * decision.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review, not a regression target. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=RsiOversoldRecalibrationTest}) for the actual figures.
 */
class RsiOversoldRecalibrationTest {

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");
    private static final List<Candle> SOLUSDT = BacktestCandleCsvLoader.load("backtest/solusdt-daily-history.csv");

    /** Matches {@code OutOfSampleValidationTest}'s own tuning/held-out split exactly. */
    private static final int SPLIT_INDEX = 700;

    private static final List<Candle> BTCUSDT_TUNING = BTCUSDT.subList(0, SPLIT_INDEX);
    private static final List<Candle> DOGEUSDT_TUNING = DOGEUSDT.subList(0, SPLIT_INDEX);
    private static final List<Candle> BTCUSDT_HELD_OUT = BTCUSDT.subList(SPLIT_INDEX, BTCUSDT.size());
    private static final List<Candle> DOGEUSDT_HELD_OUT = DOGEUSDT.subList(SPLIT_INDEX, DOGEUSDT.size());

    private static final RuleThresholds DEFAULT = RuleThresholds.DEFAULT;

    /** rsiOverbought fixed at 75 throughout — already OOS-validated by E8-F4-S1. 25 and 30 are
     * carried as in-grid controls (already-known values, now measured on the tuning-window slice
     * specifically, which neither prior test used). 26-29 fill the gap between them; 32 is the one
     * past-30 check, since 30 was never itself the product of a search — it was the original
     * hand-picked pre-tuning value. */
    private static final List<BigDecimal> CANDIDATE_OVERSOLD_VALUES = List.of(
            new BigDecimal("24"), new BigDecimal("25"), new BigDecimal("26"), new BigDecimal("27"),
            new BigDecimal("28"), new BigDecimal("29"), new BigDecimal("30"), new BigDecimal("32"));

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void sweepRsiOversoldOnTuningWindowOnly() {
        System.out.println();
        System.out.println("########## E8-F1-S2: rsiOversold sweep, TUNING WINDOW ONLY (first " + SPLIT_INDEX + " candles) ##########");

        for (BigDecimal oversold : CANDIDATE_OVERSOLD_VALUES) {
            RuleThresholds candidate = thresholdsFor(oversold);
            String label = candidateLabel(oversold);

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
        System.out.println("########## E8-F1-S2: every rsiOversold candidate vs. held-out BTCUSDT/DOGEUSDT tails + untouched SOLUSDT ##########");

        for (BigDecimal oversold : CANDIDATE_OVERSOLD_VALUES) {
            RuleThresholds candidate = thresholdsFor(oversold);
            String label = candidateLabel(oversold);

            runAndPrint("BTCUSDT [held-out tail]", BTCUSDT_HELD_OUT, label, candidate);
            runAndPrint("DOGEUSDT [held-out tail]", DOGEUSDT_HELD_OUT, label, candidate);
            runAndPrint("SOLUSDT [untouched fixture]", SOLUSDT, label, candidate);
        }
    }

    private String candidateLabel(BigDecimal oversold) {
        String suffix = oversold.compareTo(new BigDecimal("25")) == 0 ? " (v2, current)"
                : oversold.compareTo(new BigDecimal("30")) == 0 ? " (pre-tuning)" : "";
        return oversold + "/75" + suffix;
    }

    private RuleThresholds thresholdsFor(BigDecimal oversold) {
        return new RuleThresholds(oversold, SignalRuleEngine.RSI_OVERBOUGHT_THRESHOLD,
                DEFAULT.volatilityExtreme(), DEFAULT.volumeDriedUp());
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

    /** Prints both raw and after-cost expectancy per checkpoint — unlike {@code
     * ThresholdCalibrationTest}/{@code OutOfSampleValidationTest} (which only print raw), this
     * story's ship bar is decided on {@code expectancyPctAfterCosts()} specifically, so it needs
     * to be visible inline, not just in a separate summary line. */
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
