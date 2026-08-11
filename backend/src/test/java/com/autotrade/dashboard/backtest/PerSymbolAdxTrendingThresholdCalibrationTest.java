package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.RegimeClassifier;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S4: calibrates {@code RegimeClassifier.ADX_TRENDING_THRESHOLD} <b>per ticker symbol</b>,
 * on the BUY side only, per this story's AC — the mechanism E8-F3-S4 was chartered to try after
 * E8-F4-S2's out-of-sample check found the BUY-side regime effect is fixture-dependent (ranging
 * beats trending on BTCUSDT/DOGEUSDT, trending beats ranging on SOLUSDT), the same asset-divergent
 * shape E8-F1-S3 found for {@code rsiOverbought} before E8-F1-S4's per-symbol override resolved it
 * there.
 *
 * <p>Same tune-then-validate-independently-per-symbol design {@code
 * PerSymbolRsiOverboughtCalibrationTest} (E8-F1-S4) established, reusing {@link FixtureSplits}'
 * existing 70/30 chronological split verbatim (no new fixture):
 * <ol>
 *   <li>{@link #sweepEachSymbolOnItsOwnTuningWindow()} sweeps {@link #CANDIDATE_THRESHOLDS} against
 *   each of BTCUSDT/DOGEUSDT/SOLUSDT's own first {@link FixtureSplits#SPLIT_INDEX} candles only,
 *   printing the BUY-side trending/ranging split ({@code buyByRegime()}) per candidate.</li>
 *   <li>{@link #validateEachSymbolOnItsOwnHeldOutTail()} replays every candidate for a symbol
 *   against that <b>same</b> symbol's own held-out tail (candles 700-1000) only.</li>
 * </ol>
 *
 * <p>The candidate grid ({@link #CANDIDATE_THRESHOLDS}) was sized from a throwaway probe (run
 * once, deleted before committing — same precedent E8-F1-S5/S6 used for their own grids) of real
 * ADX values across each fixture's own tuning window: BTCUSDT ranged roughly 9.4-56.9 (median
 * 22.4), DOGEUSDT roughly 9.5-67.9 (median 26.4), SOLUSDT roughly 10.2-52.1 (median 24.3) — all
 * three medians cluster close to the current global default (25), so the grid brackets it broadly
 * enough to plausibly flip which regime bucket most decision points fall into.
 *
 * <p><b>Ship bar, per symbol independently</b> (adapted from {@code
 * PerSymbolRsiOverboughtCalibrationTest}'s bar — this story isn't "beat a fixed baseline value" but
 * "find a threshold that usefully separates BUY quality by regime"): a symbol ships an override
 * only if some candidate's BUY-side trending-bucket after-cost expectancy
 * ({@code buyByRegime().trending().*.expectancyPctAfterCosts()}) exceeds its ranging-bucket
 * after-cost expectancy at every one of MIN/MID/MAX on that symbol's own tuning window, with
 * non-degenerate {@code n} in both buckets (rejecting a candidate that pushes one bucket to ~0,
 * which would trivially "win" by leaving no opposing evidence — the same degenerate-comparison risk
 * {@code PerSymbolRsiOverboughtCalibrationTest}'s "comparable n" language guards against), <i>and</i>
 * that same candidate's trending-beats-ranging gap still holds at every checkpoint on that symbol's
 * own held-out tail with comparable bucket sizes. A symbol whose tuning sweep produces no qualifying
 * candidate, or whose winner doesn't confirm on its own held-out tail, ships no override and falls
 * back to {@link RegimeClassifier#ADX_TRENDING_THRESHOLD} — a legitimate per-symbol no-ship, the
 * same treatment prior E8-F1 no-ship stories gave their own outcomes. See docs/CHANGELOG.md's
 * E8-F3-S4 entry for the actual per-symbol figures and decisions.
 *
 * <p><b>Actual result: all three symbols ship no override.</b> BTCUSDT's tuning window does
 * produce qualifying winners (ADX&gt;=25/28/30 each have trending uniformly beating ranging's
 * after-cost expectancy at every checkpoint, with non-degenerate {@code n} in both buckets) — but
 * every one of those reverses on BTCUSDT's own held-out tail, where ranging beats trending instead
 * at the same thresholds; the only held-out-tail candidate where trending wins (ADX&gt;=35) has a
 * degenerate trending bucket (n=3). DOGEUSDT and SOLUSDT both fail earlier: ranging beats trending
 * at literally every one of the 9 swept candidates on each symbol's own tuning window, so neither
 * ever produces a tuning-window winner to validate in the first place — for SOLUSDT specifically,
 * this means its tuning window and its own held-out tail actively disagree (tuning favors ranging,
 * held-out favors trending at the default per E8-F4-S2), before any candidate is even tested.
 *
 * <p>Deliberately does <b>not</b> assert a "SELL unaffected" invariant the way {@code
 * PerSymbolRsiOverboughtCalibrationTest} does for {@code rsiOverbought}: unlike an RSI bound, a
 * swept ADX threshold reclassifies the same {@code regime} value used for both directions inside
 * {@link BacktestHarness}'s loop, so {@code sellByRegime()} moves too under a swept candidate here
 * — that isolation does not hold on this axis, and is not expected to. The "SELL gate's shipped
 * behavior is unaffected in production" guarantee instead comes from the wiring design itself (see
 * {@code RegimeClassifier#classify(BigDecimal, BigDecimal)}'s Javadoc): the SELL gate always
 * classifies against the global default via the no-arg {@code classify(BigDecimal)} overload,
 * never against a per-symbol threshold, so nothing this calibration test finds can change it.
 *
 * <p>Assertions here are structural only, mirroring every other E8 calibration test — the printed
 * report is the evidence the ship decision was actually made from. Read the printed output (rerun
 * via {@code ./mvnw test -Dtest=PerSymbolAdxTrendingThresholdCalibrationTest}) for the actual
 * figures.
 */
class PerSymbolAdxTrendingThresholdCalibrationTest {

    /** Sized from a throwaway probe of real per-fixture ADX distributions — see class Javadoc. */
    private static final List<BigDecimal> CANDIDATE_THRESHOLDS = List.of(
            new BigDecimal("15"), new BigDecimal("18"), new BigDecimal("20"), new BigDecimal("22"),
            RegimeClassifier.ADX_TRENDING_THRESHOLD, new BigDecimal("28"), new BigDecimal("30"),
            new BigDecimal("35"), new BigDecimal("40"));

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    @Test
    void sweepEachSymbolOnItsOwnTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F3-S4: ADX trending-threshold swept per symbol, TUNING WINDOW ONLY (first "
                + FixtureSplits.SPLIT_INDEX + " candles each) ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [tuning] ----");
            for (BigDecimal threshold : CANDIDATE_THRESHOLDS) {
                runAndPrint(symbol.name() + " [tuning]", symbol.tuning(), threshold);
            }
        }
    }

    @Test
    void validateEachSymbolOnItsOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F3-S4: every ADX-threshold candidate vs. that SAME symbol's own held-out tail ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            System.out.println();
            System.out.println("---- " + symbol.name() + " [held-out tail] ----");
            for (BigDecimal threshold : CANDIDATE_THRESHOLDS) {
                runAndPrint(symbol.name() + " [held-out tail]", symbol.heldOut(), threshold);
            }
        }
    }

    private String candidateLabel(BigDecimal threshold) {
        String suffix = threshold.compareTo(RegimeClassifier.ADX_TRENDING_THRESHOLD) == 0 ? " (current default)" : "";
        return "ADX>=" + threshold + suffix;
    }

    private void runAndPrint(String symbolLabel, List<Candle> candles, BigDecimal threshold) {
        BacktestReport report = BacktestHarness.run(symbolLabel + " [" + candidateLabel(threshold) + "]", candles, threshold);
        System.out.println();
        System.out.println(report.label() + " (n buy trending=" + report.buyByRegime().trending().totalCalls()
                + ", n buy ranging=" + report.buyByRegime().ranging().totalCalls() + "):");
        printLine("  BUY  trending", report.buyByRegime().trending());
        printLine("  BUY  ranging ", report.buyByRegime().ranging());
        printLine("  SELL trending", report.sellByRegime().trending());
        printLine("  SELL ranging ", report.sellByRegime().ranging());

        assertEquals(report.overallBuy().totalCalls(),
                report.buyByRegime().trending().totalCalls() + report.buyByRegime().ranging().totalCalls(),
                report.label() + ": BUY regime split must partition overallBuy's total calls exactly");
        assertEquals(report.overallSell().totalCalls(),
                report.sellByRegime().trending().totalCalls() + report.sellByRegime().ranging().totalCalls(),
                report.label() + ": SELL regime split must partition overallSell's total calls exactly");
        assertStructurallySane(report.label() + " BUY trending", report.buyByRegime().trending());
        assertStructurallySane(report.label() + " BUY ranging", report.buyByRegime().ranging());
        assertStructurallySane(report.label() + " SELL trending", report.sellByRegime().trending());
        assertStructurallySane(report.label() + " SELL ranging", report.sellByRegime().ranging());
    }

    private void printLine(String label, DirectionalOutcomeStats stats) {
        if (stats.totalCalls() == 0) {
            System.out.printf("%s (n=0)%n", label);
            return;
        }
        System.out.printf("%s (n=%d)%n", label, stats.totalCalls());
        printCheckpoint("    min", stats.min());
        printCheckpoint("    mid", stats.mid());
        printCheckpoint("    max", stats.max());
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
            assertTrue(cp.expectancyPctAfterCosts() <= cp.expectancyPct(),
                    label + ": after-cost expectancy must never exceed raw expectancy");
        }
    }
}
