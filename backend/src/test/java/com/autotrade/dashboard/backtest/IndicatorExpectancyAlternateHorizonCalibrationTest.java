package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.IndicatorId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F3-S5: re-attempts {@code WeightedVoteRuleEngine.IndicatorWeights.DEFAULT}'s calibration at
 * alternate horizon/TP-SL combinations, since {@link IndicatorExpectancyCalibrationTest}'s
 * all-zero finding (E8-F3-S1, confirmed out-of-sample by {@link OutOfSampleValidationTest},
 * E8-F4-S1) was only ever measured under one fixed {@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}
 * (5 days) / {@link BacktestConfig#TAKE_PROFIT_PCT} (5%) / {@link BacktestConfig#STOP_LOSS_PCT}
 * (3%) setup — a result that could reflect that specific short horizon rather than every indicator
 * being unprofitable at every horizon, per {@code IndicatorWeights.DEFAULT}'s own Javadoc note
 * ("a future recalibration, e.g. against a longer horizon, produces a positive weight").
 *
 * <p>Two alternate candidates, both anchored to real values already in this codebase rather than
 * arbitrary picks: 10 days (double the baseline) and 15 days (the longest {@code HoldTermRule}
 * hold-term upper bound, {@code STRONG_LOW}'s {@code maxDays}). TP/SL is scaled proportionally
 * with the horizon (10 days -&gt; 10%/6%, 15 days -&gt; 15%/9%) rather than held at the baseline's
 * 5%/3% — holding TP/SL fixed while only lengthening the horizon would just mean more decision
 * points fall back to horizon-expiry scoring instead of resolving via a genuine TP/SL crossing,
 * which doesn't test a materially different bracket, just a laxer one.
 *
 * <p>Same tuning fixtures (full BTCUSDT/DOGEUSDT) and same combine-across-fixtures treatment
 * ({@link BacktestHarness#combineCheckpoint}) as the original {@link
 * IndicatorExpectancyCalibrationTest} this re-attempts, so the two are a direct apples-to-apples
 * comparison at a different horizon, not a differently-scoped re-run.
 *
 * <p><b>Ship bar (confirmed with the user before implementation), per this story's AC:</b> if any
 * indicator's combined after-cost expectancy comes back positive under either alternate
 * configuration, {@code IndicatorWeights.DEFAULT} is recalibrated from it and validated
 * out-of-sample per {@link OutOfSampleValidationTest}'s own methodology before shipping;
 * confirming the original all-zero finding at every tested horizon is an equally valid, documented
 * ending, per every prior E8 calibration story's own no-ship precedent.
 *
 * <p>Assertions here are structural only, mirroring {@link IndicatorExpectancyCalibrationTest} —
 * the printed report is the evidence under review, not a regression target. Read the printed
 * output (rerun via {@code ./mvnw test -Dtest=IndicatorExpectancyAlternateHorizonCalibrationTest})
 * for the actual figures.
 */
class IndicatorExpectancyAlternateHorizonCalibrationTest {

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");

    private record HorizonCandidate(String label, int horizonDays, BigDecimal takeProfitPct, BigDecimal stopLossPct) {
    }

    private static final List<HorizonCandidate> CANDIDATES = List.of(
            new HorizonCandidate("10 days (2x baseline), TP 10% / SL 6%", 10, new BigDecimal("10.0"), new BigDecimal("6.0")),
            new HorizonCandidate("15 days (HoldTermRule.STRONG_LOW max), TP 15% / SL 9%", 15, new BigDecimal("15.0"), new BigDecimal("9.0")));

    @Test
    void printPerIndicatorExpectancyAtAlternateHorizons() {
        System.out.println();
        System.out.println("########## E8-F3-S5: per-indicator expectancy at alternate horizons (baseline: 5 days, TP 5% / SL 3%) ##########");

        for (HorizonCandidate candidate : CANDIDATES) {
            System.out.printf("%n=== %s ===%n", candidate.label());

            var btcStatsByIndicator = BacktestHarness.runIndicatorExpectancy(BTCUSDT, candidate.horizonDays(),
                    candidate.takeProfitPct(), candidate.stopLossPct());
            var dogeStatsByIndicator = BacktestHarness.runIndicatorExpectancy(DOGEUSDT, candidate.horizonDays(),
                    candidate.takeProfitPct(), candidate.stopLossPct());

            for (IndicatorId indicatorId : IndicatorId.values()) {
                CheckpointStats btcStats = btcStatsByIndicator.get(indicatorId);
                CheckpointStats dogeStats = dogeStatsByIndicator.get(indicatorId);
                CheckpointStats combined = BacktestHarness.combineCheckpoint(btcStats, dogeStats);

                System.out.printf("%n%s:%n", indicatorId);
                printLine("  BTCUSDT ", btcStats);
                printLine("  DOGEUSDT", dogeStats);
                printLine("  COMBINED", combined);
                System.out.printf("  -> weight = max(0, %.4f) = %.4f%n", combined.expectancyPctAfterCosts(),
                        Math.max(0.0, combined.expectancyPctAfterCosts()));

                assertStructurallySane(candidate.label() + " " + indicatorId, btcStats);
                assertStructurallySane(candidate.label() + " " + indicatorId, dogeStats);
            }
        }
    }

    private void printLine(String label, CheckpointStats stats) {
        System.out.printf("%s: %5.1f%% win (%d scored, %d n) | avg win %+6.2f%% | avg loss %+6.2f%% | expectancy %+6.3f%% (after costs %+6.3f%%) | tpHit=%d slHit=%d horizonExpired=%d%n",
                label, stats.winRate(), stats.scored(), stats.scored() + stats.notScored(), stats.avgWinReturnPct(),
                stats.avgLossReturnPct(), stats.expectancyPct(), stats.expectancyPctAfterCosts(), stats.tpHit(),
                stats.slHit(), stats.horizonExpired());
    }

    /**
     * The tuning run above found both MACD and MA_CROSSOVER come back with a real positive
     * combined after-cost expectancy at the 15-day/TP15%/SL9% candidate (MACD +0.7135, MA_CROSSOVER
     * +0.1623 — RSI stays negative at every horizon tested). Per this story's confirmed ship bar,
     * that isn't shipped on tuning-set evidence alone: validated here against the same held-out
     * BTCUSDT/DOGEUSDT tails plus the untouched SOLUSDT fixture {@link OutOfSampleValidationTest}
     * (E8-F4-S1) used for the original 5-day calibration, at the same 15-day/TP15%/SL9% horizon.
     */
    @Test
    void indicatorWeightsHoldOutOfSampleAtFifteenDayHorizon() {
        System.out.println();
        System.out.println("########## E8-F3-S5: per-indicator expectancy at 15-day/TP15%/SL9% horizon, held-out data ##########");

        HorizonCandidate winner = CANDIDATES.get(1);
        var btcStats = BacktestHarness.runIndicatorExpectancy(FixtureSplits.BTCUSDT_HELD_OUT, winner.horizonDays(),
                winner.takeProfitPct(), winner.stopLossPct());
        var dogeStats = BacktestHarness.runIndicatorExpectancy(FixtureSplits.DOGEUSDT_HELD_OUT, winner.horizonDays(),
                winner.takeProfitPct(), winner.stopLossPct());
        var solStats = BacktestHarness.runIndicatorExpectancy(FixtureSplits.SOLUSDT, winner.horizonDays(),
                winner.takeProfitPct(), winner.stopLossPct());

        for (IndicatorId indicatorId : IndicatorId.values()) {
            CheckpointStats btc = btcStats.get(indicatorId);
            CheckpointStats doge = dogeStats.get(indicatorId);
            CheckpointStats sol = solStats.get(indicatorId);
            CheckpointStats combined = BacktestHarness.combineCheckpoint(BacktestHarness.combineCheckpoint(btc, doge), sol);

            System.out.printf("%n%s:%n", indicatorId);
            printLine("  BTCUSDT [held-out] ", btc);
            printLine("  DOGEUSDT [held-out]", doge);
            printLine("  SOLUSDT [untouched]", sol);
            printLine("  COMBINED            ", combined);
            System.out.printf("  -> out-of-sample weight = max(0, %.4f) = %.4f (tuning-set weight was %.4f)%n",
                    combined.expectancyPctAfterCosts(), Math.max(0.0, combined.expectancyPctAfterCosts()),
                    tuningSetWeightFor(indicatorId));

            assertStructurallySane("held-out 15d " + indicatorId, btc);
            assertStructurallySane("held-out 15d " + indicatorId, doge);
            assertStructurallySane("held-out 15d " + indicatorId, sol);
        }
    }

    private double tuningSetWeightFor(IndicatorId indicatorId) {
        return switch (indicatorId) {
            case RSI -> 0.0000;
            case MACD -> 0.7135;
            case MA_CROSSOVER -> 0.1623;
        };
    }

    /** Same structural invariants {@link IndicatorExpectancyCalibrationTest} already checks. */
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
