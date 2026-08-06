package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.RegimeClassifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S2: runs {@link BacktestHarness}'s new regime-split scoring against the same checked-in
 * BTCUSDT/DOGEUSDT fixtures {@link BacktestHarnessTest} (E2-F4-S1/S2), {@code
 * ThresholdCalibrationTest} (E8-F1-S1), and {@code IndicatorExpectancyCalibrationTest}/{@code
 * WeightedVoteBacktestTest} (E8-F3-S1) already use, and prints trending-vs-ranging win
 * rate/expectancy for both directions — the direct "does a regime filter earn its keep" evidence
 * this story's AC asks for, and the evidence {@code RegimeGatedRuleEngine}'s eventual production
 * wiring decision (currently deferred, see its class Javadoc) is gated on.
 *
 * <p><b>Overfitting caveat (deliberate scope boundary), same as every other E8 calibration
 * test:</b> both fixtures are the only evidence this pass has; {@link
 * RegimeClassifier#ADX_TRENDING_THRESHOLD} is an uncalibrated industry-default placeholder, not
 * backtest-derived, and this pass doesn't attempt to calibrate it. E8-F4-S1's out-of-sample
 * validation pass covered E8-F1-S1's threshold shift and {@code WeightedVoteRuleEngine}'s
 * weights but explicitly left this story's regime filter out of scope (its calibration was
 * already fixture-mixed rather than a clean value to validate) — still unvalidated
 * out-of-sample, pending a future story.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review, not a regression target. Read the printed output (rerun
 * via {@code ./mvnw test -Dtest=RegimeCalibrationTest}) for the actual figures.
 */
class RegimeCalibrationTest {

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");

    @Test
    void printRegimeSplitExpectancy_btcUsdt() {
        printAndVerify("BTCUSDT", BTCUSDT);
    }

    @Test
    void printRegimeSplitExpectancy_dogeUsdt() {
        printAndVerify("DOGEUSDT", DOGEUSDT);
    }

    private void printAndVerify(String symbol, List<Candle> candles) {
        BacktestReport report = BacktestHarness.run(symbol, candles);

        System.out.printf("%n########## Regime-split expectancy (%s, ADX >= %s = TRENDING) ##########%n",
                symbol, RegimeClassifier.ADX_TRENDING_THRESHOLD);
        printLine("BUY  trending", report.buyByRegime().trending());
        printLine("BUY  ranging ", report.buyByRegime().ranging());
        printLine("SELL trending", report.sellByRegime().trending());
        printLine("SELL ranging ", report.sellByRegime().ranging());

        // The regime split is additive over the harness's existing per-rule accumulation: every
        // BUY/SELL decision point that landed in overallBuy/overallSell also landed in exactly
        // one of {trending, ranging} for that same direction — a partition, not a resample.
        assertEquals(report.overallBuy().totalCalls(),
                report.buyByRegime().trending().totalCalls() + report.buyByRegime().ranging().totalCalls(),
                symbol + ": BUY regime split must partition overallBuy's total calls exactly");
        assertEquals(report.overallSell().totalCalls(),
                report.sellByRegime().trending().totalCalls() + report.sellByRegime().ranging().totalCalls(),
                symbol + ": SELL regime split must partition overallSell's total calls exactly");

        assertStructurallySane(symbol + " BUY trending", report.buyByRegime().trending());
        assertStructurallySane(symbol + " BUY ranging", report.buyByRegime().ranging());
        assertStructurallySane(symbol + " SELL trending", report.sellByRegime().trending());
        assertStructurallySane(symbol + " SELL ranging", report.sellByRegime().ranging());
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
