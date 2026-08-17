package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.BacktestCandleCsvLoader;
import com.autotrade.dashboard.backtest.BacktestHarness;
import com.autotrade.dashboard.backtest.BacktestReport;
import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.backtest.CheckpointStats;
import com.autotrade.dashboard.backtest.DirectionalOutcomeStats;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.RegimeClassifier;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * E8-F5-S1: re-derives {@link LiveDriftBaseline}'s hardcoded v2 BUY/SELL {@code
 * expectancyPctAfterCosts} constants from the real checked-in BTCUSDT/DOGEUSDT fixtures and pins
 * them down within a documented tolerance — the same "computed once, pinned as a constant,
 * guarded by a re-deriving test" pattern {@code IndicatorExpectancyCalibrationTest} (E8-F3-S1)
 * already established for {@code WeightedVoteRuleEngine.IndicatorWeights.DEFAULT}.
 *
 * <p>Combines {@code BacktestHarness.run}'s {@code overallBuy()}/{@code overallSell()} across
 * both fixtures call-count-weighted (win/loss-count-weighted average of avg win/loss size,
 * matching {@code BacktestHarness.combineCheckpoint}'s formula — reimplemented here rather than
 * called directly, since that method is package-private to {@code backtest} and this test lives
 * in {@code monitoring}). {@link #TOLERANCE} exists only to absorb the printf-rounding gap
 * between the console output {@link LiveDriftBaseline}'s constants were transcribed from (6
 * decimal places) and this test's full double precision — not because the underlying computation
 * is expected to be nondeterministic. A genuine mismatch beyond that tolerance means {@code
 * BacktestHarness}, the fixtures, or {@code SignalRuleEngine.RuleThresholds.DEFAULT} changed
 * since {@link LiveDriftBaseline} was computed, and the constants need re-deriving.
 *
 * <p>E8-F5-S2 added the funding-adjusted BUY/SELL re-derivations below, same methodology, reading
 * {@code expectancyPctAfterCostsAndFunding()} instead of {@code expectancyPctAfterCosts()}.
 */
class LiveDriftBaselineTest {

    private static final double TOLERANCE = 1e-4;

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");

    @Test
    void buyBaselineMatchesCombinedFixtureComputation() {
        BacktestReport btc = BacktestHarness.run("BTCUSDT", BTCUSDT);
        BacktestReport doge = BacktestHarness.run("DOGEUSDT", DOGEUSDT);

        assertCombinedMatches(btc.overallBuy(), doge.overallBuy(),
                LiveDriftBaseline.BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.BUY_MID_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS);

        assertEquals(LiveDriftBaseline.BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.expectancyPctAfterCosts(true, Checkpoint.MIN), TOLERANCE);
        assertEquals(LiveDriftBaseline.BUY_MID_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.expectancyPctAfterCosts(true, Checkpoint.MID), TOLERANCE);
        assertEquals(LiveDriftBaseline.BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.expectancyPctAfterCosts(true, Checkpoint.MAX), TOLERANCE);
    }

    /** E8-F5-S2: same combined-fixture derivation as {@link #buyBaselineMatchesCombinedFixtureComputation()},
     * but reading {@code expectancyPctAfterCostsAndFunding()} instead of {@code
     * expectancyPctAfterCosts()} — re-derives {@link LiveDriftBaseline}'s new funding-adjusted BUY
     * constants. */
    @Test
    void buyFundingAdjustedBaselineMatchesCombinedFixtureComputation() {
        BacktestReport btc = BacktestHarness.run("BTCUSDT", BTCUSDT);
        BacktestReport doge = BacktestHarness.run("DOGEUSDT", DOGEUSDT);

        assertCombinedFundingMatches(btc.overallBuy(), doge.overallBuy(),
                LiveDriftBaseline.BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.BUY_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING);

        assertEquals(LiveDriftBaseline.BUY_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.expectancyPctAfterCostsAndFunding(true, Checkpoint.MIN), TOLERANCE);
        assertEquals(LiveDriftBaseline.BUY_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.expectancyPctAfterCostsAndFunding(true, Checkpoint.MID), TOLERANCE);
        assertEquals(LiveDriftBaseline.BUY_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.expectancyPctAfterCostsAndFunding(true, Checkpoint.MAX), TOLERANCE);
    }

    /** E8-F1-S11: the SELL baseline now must reflect both wired SELL-only gates' combined behavior
     * — a live SELL audit entry can only ever be a trending-regime call whose MA-crossover
     * separation also clears 2.00%, so the baseline must be recomputed with both gates applied
     * ({@code applySellRegimeGate=true}, {@code applyMaCrossoverSellGate=true}), not just relabeled,
     * the same "recompute, don't just relabel" treatment E8-F3-S3's v3&rarr;v4 bump got. */
    @Test
    void sellBaselineMatchesCombinedFixtureComputation() {
        BacktestReport btc = BacktestHarness.run("BTCUSDT", BTCUSDT, SignalRuleEngine::evaluate,
                SignalRuleEngine.RuleThresholds.DEFAULT, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);
        BacktestReport doge = BacktestHarness.run("DOGEUSDT", DOGEUSDT, SignalRuleEngine::evaluate,
                SignalRuleEngine.RuleThresholds.DEFAULT, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);

        assertCombinedMatches(btc.overallSell(), doge.overallSell(),
                LiveDriftBaseline.SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.SELL_MID_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS);

        assertEquals(LiveDriftBaseline.SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.expectancyPctAfterCosts(false, Checkpoint.MIN), TOLERANCE);
        assertEquals(LiveDriftBaseline.SELL_MID_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.expectancyPctAfterCosts(false, Checkpoint.MID), TOLERANCE);
        assertEquals(LiveDriftBaseline.SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS,
                LiveDriftBaseline.expectancyPctAfterCosts(false, Checkpoint.MAX), TOLERANCE);
    }

    /** E8-F5-S2: same both-gates-applied derivation as {@link #sellBaselineMatchesCombinedFixtureComputation()},
     * but reading {@code expectancyPctAfterCostsAndFunding()} instead of {@code
     * expectancyPctAfterCosts()} — re-derives {@link LiveDriftBaseline}'s new funding-adjusted
     * SELL constants. */
    @Test
    void sellFundingAdjustedBaselineMatchesCombinedFixtureComputation() {
        BacktestReport btc = BacktestHarness.run("BTCUSDT", BTCUSDT, SignalRuleEngine::evaluate,
                SignalRuleEngine.RuleThresholds.DEFAULT, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);
        BacktestReport doge = BacktestHarness.run("DOGEUSDT", DOGEUSDT, SignalRuleEngine::evaluate,
                SignalRuleEngine.RuleThresholds.DEFAULT, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);

        assertCombinedFundingMatches(btc.overallSell(), doge.overallSell(),
                LiveDriftBaseline.SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.SELL_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING);

        assertEquals(LiveDriftBaseline.SELL_MIN_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.expectancyPctAfterCostsAndFunding(false, Checkpoint.MIN), TOLERANCE);
        assertEquals(LiveDriftBaseline.SELL_MID_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.expectancyPctAfterCostsAndFunding(false, Checkpoint.MID), TOLERANCE);
        assertEquals(LiveDriftBaseline.SELL_MAX_EXPECTANCY_PCT_AFTER_COSTS_AND_FUNDING,
                LiveDriftBaseline.expectancyPctAfterCostsAndFunding(false, Checkpoint.MAX), TOLERANCE);
    }

    private void assertCombinedMatches(DirectionalOutcomeStats btc, DirectionalOutcomeStats doge,
                                        double expectedMin, double expectedMid, double expectedMax) {
        assertEquals(expectedMin, combine(btc.min(), doge.min()).expectancyPctAfterCosts(), TOLERANCE, "min checkpoint");
        assertEquals(expectedMid, combine(btc.mid(), doge.mid()).expectancyPctAfterCosts(), TOLERANCE, "mid checkpoint");
        assertEquals(expectedMax, combine(btc.max(), doge.max()).expectancyPctAfterCosts(), TOLERANCE, "max checkpoint");
    }

    private void assertCombinedFundingMatches(DirectionalOutcomeStats btc, DirectionalOutcomeStats doge,
                                               double expectedMin, double expectedMid, double expectedMax) {
        assertEquals(expectedMin, combine(btc.min(), doge.min()).expectancyPctAfterCostsAndFunding(), TOLERANCE, "min checkpoint");
        assertEquals(expectedMid, combine(btc.mid(), doge.mid()).expectancyPctAfterCostsAndFunding(), TOLERANCE, "mid checkpoint");
        assertEquals(expectedMax, combine(btc.max(), doge.max()).expectancyPctAfterCostsAndFunding(), TOLERANCE, "max checkpoint");
    }

    /** Same call-count-weighted combine formula as {@code BacktestHarness.combineCheckpoint}
     * (package-private, not accessible from this package) — reimplemented here since {@link
     * CheckpointStats} is a plain public record with no combine method of its own. */
    private CheckpointStats combine(CheckpointStats a, CheckpointStats b) {
        int win = a.win() + b.win();
        int loss = a.loss() + b.loss();
        int scored = win + loss + a.wash() + b.wash();
        double avgWin = win == 0 ? 0.0 : (a.avgWinReturnPct() * a.win() + b.avgWinReturnPct() * b.win()) / win;
        double avgLoss = loss == 0 ? 0.0 : (a.avgLossReturnPct() * a.loss() + b.avgLossReturnPct() * b.loss()) / loss;
        double avgHoldingDays = scored == 0 ? 0.0
                : (a.avgHoldingDays() * a.scored() + b.avgHoldingDays() * b.scored()) / scored;
        return new CheckpointStats(win, loss, a.wash() + b.wash(), a.notScored() + b.notScored(), avgWin, avgLoss,
                a.tpHit() + b.tpHit(), a.slHit() + b.slHit(), a.horizonExpired() + b.horizonExpired(), avgHoldingDays);
    }
}
