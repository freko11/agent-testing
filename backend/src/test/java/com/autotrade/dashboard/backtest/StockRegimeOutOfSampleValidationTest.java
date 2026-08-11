package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.RegimeClassifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F1-S7: checks whether {@code RegimeGatedRuleEngine.applySellGate}'s already-shipped,
 * crypto-wide evidence (trending beats ranging on SELL-side after-cost expectancy, uniformly
 * across BTCUSDT/DOGEUSDT/SOLUSDT — {@code RegimeOutOfSampleValidationTest}, E8-F4-S2) also holds
 * for a stock, using AAPL's held-out tail ({@link FixtureSplits#AAPL_HELD_OUT}).
 *
 * <p>Validation-only, no tuning-window phase — same shape as {@code RegimeOutOfSampleValidationTest}
 * itself, for the same reason: {@link RegimeClassifier#ADX_TRENDING_THRESHOLD} was fixed a priori
 * as an industry rule-of-thumb and was never tuned against any fixture, crypto or stock, so there is
 * nothing to "validate" beyond a plain held-out check at the existing global threshold. A per-symbol
 * ADX sweep for AAPL (mirroring {@code PerSymbolAdxTrendingThresholdCalibrationTest}, E8-F3-S4) is a
 * different, not-requested mechanism, out of scope for this story's AC.
 *
 * <p><b>Wiring decision:</b> {@code RegimeGatedRuleEngine.sellGateAppliesTo(AssetType)} stays a
 * crypto-only check regardless of this story's outcome — one confirming stock symbol is not
 * evidence a mechanism generalizes to "all stocks" the way three-for-three crypto symbols supported
 * generalizing to "all crypto tested"; extending the asset-type-wide check off a single symbol would
 * repeat exactly the extrapolation this story's own premise says not to make. A confirming result
 * here would instead be evidence for a narrow, AAPL-specific extension (mirroring {@code
 * applyBuyGate}/{@code buyGateAppliesTo(String)}'s per-symbol allow-list shape), not a widened
 * {@code AssetType} check. See docs/CHANGELOG.md's E8-F1-S7 entry for the actual figures and the
 * resulting decision.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence under review. Read the printed output (rerun via
 * {@code ./mvnw test -Dtest=StockRegimeOutOfSampleValidationTest}) for the actual figures.
 */
class StockRegimeOutOfSampleValidationTest {

    @Test
    void printRegimeSplitExpectancy_aaplHeldOut() {
        printAndVerify("AAPL [held-out tail]", FixtureSplits.AAPL_HELD_OUT);
    }

    private void printAndVerify(String symbolLabel, List<Candle> candles) {
        BacktestReport report = BacktestHarness.run(symbolLabel, candles);

        System.out.printf("%n########## E8-F1-S7: regime-split expectancy, held-out tail (%s, ADX >= %s = TRENDING) ##########%n",
                symbolLabel, RegimeClassifier.ADX_TRENDING_THRESHOLD);
        printLine("BUY  trending", report.buyByRegime().trending());
        printLine("BUY  ranging ", report.buyByRegime().ranging());
        printLine("SELL trending", report.sellByRegime().trending());
        printLine("SELL ranging ", report.sellByRegime().ranging());

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
