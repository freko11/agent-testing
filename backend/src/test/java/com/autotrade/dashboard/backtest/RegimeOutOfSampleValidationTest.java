package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.RegimeClassifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F4-S2: out-of-sample validation of {@code RegimeGatedRuleEngine} (E8-F3-S2), the one E8-F3
 * mechanism {@code OutOfSampleValidationTest} (E8-F4-S1) explicitly left out of scope ("not named
 * in this story's AC... its calibration was already fixture-mixed rather than a clean value to
 * validate").
 *
 * <p>Reuses {@link FixtureSplits}'s existing chronological tune/held-out split
 * (BTCUSDT/DOGEUSDT/SOLUSDT, {@link FixtureSplits#SPLIT_INDEX}) rather than introducing a new
 * fixture or split — confirmed against this story's AC as genuine held-out evidence for this
 * specific mechanism, since {@link RegimeClassifier#ADX_TRENDING_THRESHOLD} was fixed a priori as
 * an industry rule-of-thumb (see that class's Javadoc) and was never tuned against any of these
 * three fixtures, unlike {@code SignalRuleEngine}'s RSI thresholds or {@code
 * WeightedVoteRuleEngine}'s indicator weights. That also means there is no "tuning window" run to
 * compare against here (E8-F1-S4/S5's per-symbol tests each print a tuning-window run alongside
 * the held-out one) — only the held-out tail, run through {@link BacktestHarness}'s existing
 * regime-split accumulators exactly as {@code RegimeCalibrationTest} (E8-F3-S2) does for the full
 * fixtures.
 *
 * <p><b>Wiring decision, per this story's AC:</b> {@code RegimeGatedRuleEngine} is wired into
 * {@code SignalService}/{@code OrderService} only if ranging expectancy is uniformly and
 * materially worse than trending expectancy across all three symbols' held-out tails; otherwise it
 * stays unwired, the same evidence-gated outcome {@code WeightedVoteRuleEngine} (E8-F3-S1) reached
 * with its all-zero weights. See {@code docs/CHANGELOG.md}'s E8-F4-S2 entry for the actual
 * per-symbol figures and the resulting decision.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review, not a regression target. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=RegimeOutOfSampleValidationTest}) for the actual figures.
 */
class RegimeOutOfSampleValidationTest {

    @Test
    void printRegimeSplitExpectancy_btcUsdtHeldOut() {
        printAndVerify("BTCUSDT [held-out tail]", FixtureSplits.BTCUSDT_HELD_OUT);
    }

    @Test
    void printRegimeSplitExpectancy_dogeUsdtHeldOut() {
        printAndVerify("DOGEUSDT [held-out tail]", FixtureSplits.DOGEUSDT_HELD_OUT);
    }

    @Test
    void printRegimeSplitExpectancy_solUsdtHeldOut() {
        printAndVerify("SOLUSDT [held-out tail]", FixtureSplits.SOLUSDT_HELD_OUT);
    }

    private void printAndVerify(String symbolLabel, List<Candle> candles) {
        BacktestReport report = BacktestHarness.run(symbolLabel, candles);

        System.out.printf("%n########## E8-F4-S2: regime-split expectancy, held-out tail (%s, ADX >= %s = TRENDING) ##########%n",
                symbolLabel, RegimeClassifier.ADX_TRENDING_THRESHOLD);
        printLine("BUY  trending", report.buyByRegime().trending());
        printLine("BUY  ranging ", report.buyByRegime().ranging());
        printLine("SELL trending", report.sellByRegime().trending());
        printLine("SELL ranging ", report.sellByRegime().ranging());

        // Same partition invariant RegimeCalibrationTest checks on the full fixtures: the regime
        // split is additive over the harness's existing per-rule accumulation, not a resample.
        assertEquals(report.overallBuy().totalCalls(),
                report.buyByRegime().trending().totalCalls() + report.buyByRegime().ranging().totalCalls(),
                symbolLabel + ": BUY regime split must partition overallBuy's total calls exactly");
        assertEquals(report.overallSell().totalCalls(),
                report.sellByRegime().trending().totalCalls() + report.sellByRegime().ranging().totalCalls(),
                symbolLabel + ": SELL regime split must partition overallSell's total calls exactly");

        assertStructurallySane(symbolLabel + " BUY trending", report.buyByRegime().trending());
        assertStructurallySane(symbolLabel + " BUY ranging", report.buyByRegime().ranging());
        assertStructurallySane(symbolLabel + " SELL trending", report.sellByRegime().trending());
        assertStructurallySane(symbolLabel + " SELL ranging", report.sellByRegime().ranging());
    }

    private void printLine(String label, DirectionalOutcomeStats stats) {
        if (stats.totalCalls() == 0) {
            System.out.printf("%s (n=0)%n", label);
            return;
        }
        System.out.printf("%s (n=%d)%n", label, stats.totalCalls());
        printCheckpoint("  min", stats.min());
        printCheckpoint("  mid", stats.mid());
        printCheckpoint("  max", stats.max());
    }

    private void printCheckpoint(String checkpointLabel, CheckpointStats cp) {
        System.out.printf("%s %5.1f%% win (%d scored) | avg win %+6.2f%% | avg loss %+6.2f%% | expectancy %+6.3f%% (after costs %+6.3f%%)%n",
                checkpointLabel, cp.winRate(), cp.scored(), cp.avgWinReturnPct(), cp.avgLossReturnPct(),
                cp.expectancyPct(), cp.expectancyPctAfterCosts());
    }

    /** Same structural invariants every other E8 calibration test checks. */
    private void assertStructurallySane(String label, DirectionalOutcomeStats stats) {
        for (CheckpointStats cp : List.of(stats.min(), stats.mid(), stats.max())) {
            if (cp.win() > 0) {
                assertTrue(cp.avgWinReturnPct() > 0, label + ": avg win size must be positive");
            }
            if (cp.loss() > 0) {
                assertTrue(cp.avgLossReturnPct() < 0, label + ": avg loss size must be negative");
            }
            assertEquals(cp.scored(), cp.tpHit() + cp.slHit() + cp.horizonExpired(),
                    label + ": tpHit+slHit+horizonExpired must partition scored()");
        }
    }
}
