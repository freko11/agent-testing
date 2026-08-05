package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.IndicatorId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S1: runs {@link BacktestHarness}'s new per-indicator scoring against the same checked-in
 * BTCUSDT/DOGEUSDT fixtures {@link BacktestHarnessTest} (E2-F4-S1/S2) and {@code
 * ThresholdCalibrationTest} (E8-F1-S1) already use, and prints each indicator's own
 * directional-read win rate/expectancy — the evidence {@code
 * WeightedVoteRuleEngine.IndicatorWeights.DEFAULT} is hand-calibrated from (each weight is
 * {@code max(0, combined expectancyPctAfterCosts())}, combined call-count-weighted across both
 * fixtures via {@link BacktestHarness#combineCheckpoint}).
 *
 * <p><b>Overfitting caveat (deliberate scope boundary), same as {@code ThresholdCalibrationTest}
 * and {@code SignalRuleEngine}'s RSI threshold calibration:</b> both fixtures are also this
 * pass's only tuning data, so the resulting weights are not yet validated out-of-sample — that
 * is E8-F4-S1's explicit follow-up, not attempted here. Treat {@code IndicatorWeights.DEFAULT}
 * as provisional pending that story.
 *
 * <p>Assertions here are structural only, mirroring {@link BacktestHarnessTest} — the printed
 * report is the evidence under review, not a regression target. Read the printed output (rerun
 * via {@code ./mvnw test -Dtest=IndicatorExpectancyCalibrationTest}) for the actual figures.
 */
class IndicatorExpectancyCalibrationTest {

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");

    @Test
    void printPerIndicatorExpectancy() {
        BacktestReport btc = BacktestHarness.run("BTCUSDT", BTCUSDT);
        BacktestReport doge = BacktestHarness.run("DOGEUSDT", DOGEUSDT);

        System.out.println();
        System.out.println("########## Per-indicator expectancy (calibration evidence for IndicatorWeights.DEFAULT) ##########");
        for (IndicatorId indicatorId : IndicatorId.values()) {
            CheckpointStats btcStats = btc.indicatorStats().get(indicatorId);
            CheckpointStats dogeStats = doge.indicatorStats().get(indicatorId);
            CheckpointStats combined = BacktestHarness.combineCheckpoint(btcStats, dogeStats);

            System.out.printf("%n%s:%n", indicatorId);
            printLine("  BTCUSDT ", btcStats);
            printLine("  DOGEUSDT", dogeStats);
            printLine("  COMBINED", combined);
            System.out.printf("  -> weight = max(0, %.4f) = %.4f%n", combined.expectancyPctAfterCosts(),
                    Math.max(0.0, combined.expectancyPctAfterCosts()));

            assertStructurallySane(indicatorId.name(), btcStats);
            assertStructurallySane(indicatorId.name(), dogeStats);
        }
    }

    private void printLine(String label, CheckpointStats stats) {
        System.out.printf("%s: %5.1f%% win (%d scored, %d n) | avg win %+6.2f%% | avg loss %+6.2f%% | expectancy %+6.3f%% (after costs %+6.3f%%) | tpHit=%d slHit=%d horizonExpired=%d%n",
                label, stats.winRate(), stats.scored(), stats.scored() + stats.notScored(), stats.avgWinReturnPct(),
                stats.avgLossReturnPct(), stats.expectancyPct(), stats.expectancyPctAfterCosts(), stats.tpHit(),
                stats.slHit(), stats.horizonExpired());
    }

    /** Same structural invariants {@link BacktestHarnessTest} already checks for the combined
     * rule stats, reapplied to each indicator's own single-checkpoint stats. */
    private void assertStructurallySane(String label, CheckpointStats stats) {
        if (stats.win() > 0) {
            assertTrue(stats.avgWinReturnPct() > 0, label + ": avg win size must be positive");
        }
        if (stats.loss() > 0) {
            assertTrue(stats.avgLossReturnPct() < 0, label + ": avg loss size must be negative");
        }
        assertEquals(stats.scored(), stats.tpHit() + stats.slHit() + stats.horizonExpired(),
                label + ": tpHit+slHit+horizonExpired must partition scored()");
        assertTrue(stats.expectancyPctAfterCosts() <= stats.expectancyPct(),
                label + ": after-cost expectancy must never exceed raw expectancy");
    }
}
