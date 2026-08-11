package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.IndicatorId;
import com.autotrade.dashboard.signal.RegimeClassifier;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2-F4-S1: replays the Buy/Sell/Hold rule table against real historical BTCUSDT/DOGEUSDT
 * daily candles (fetched once from Binance's public klines endpoint, checked in under
 * {@code src/test/resources/backtest/}) and prints win/loss stats — the "backtest evidence"
 * step {@code signal-rule-review}'s checklist requires before trusting the rule table with
 * real money. Crypto-only for v1: no real Alpaca paper credentials exist yet on this dev
 * machine to fetch a genuine historical stock series (per CLAUDE.md's E2-F1-S1 note), and
 * neither {@link com.autotrade.dashboard.signal.SignalRuleEngine} nor
 * {@link com.autotrade.dashboard.signal.HoldTermCalculator} branch on asset type, so this is
 * sufficient evidence for the shared rule logic — a stock series is a non-blocking follow-up
 * once Alpaca paper credentials exist (E4-F2-S1).
 *
 * <p>Assertions here are structural only (every decision point is accounted for exactly once
 * across the 9 {@link SignalRuleId} buckets) — the win rate itself is the evidence under
 * review here, not a fixed expectation to regress-test. Read the printed report (rerun via
 * {@code ./mvnw test -Dtest=BacktestHarnessTest}) for the actual finding.
 */
class BacktestHarnessTest {

    @Test
    void backtestBtcUsdt() {
        runAndVerify("BTCUSDT", "backtest/btcusdt-daily-history.csv");
    }

    @Test
    void backtestDogeUsdt() {
        runAndVerify("DOGEUSDT", "backtest/dogeusdt-daily-history.csv");
    }

    /**
     * E8-F3-S3: pins down the new {@code applySellRegimeGate} overload structurally — every
     * ranging-regime SELL call the ungated run counts (already reported by the existing
     * {@code sellByRegime} split) is exactly what the gated run reclassifies away from a
     * directional SELL bucket, and nothing else moves: BUY totals are completely untouched by the
     * gate, on both fixtures.
     */
    @Test
    void sellRegimeGate_reclassifiesExactlyTheRangingSellCalls_leavesBuyUntouched() {
        assertGatedSellReclassifiesRangingCallsOnly("BTCUSDT", "backtest/btcusdt-daily-history.csv");
        assertGatedSellReclassifiesRangingCallsOnly("DOGEUSDT", "backtest/dogeusdt-daily-history.csv");
    }

    private void assertGatedSellReclassifiesRangingCallsOnly(String label, String fixture) {
        List<Candle> candles = BacktestCandleCsvLoader.load(fixture);
        BacktestReport ungated = BacktestHarness.run(label, candles);
        BacktestReport gated = BacktestHarness.run(label, candles, SignalRuleEngine::evaluate,
                SignalRuleEngine.RuleThresholds.DEFAULT, true);

        assertEquals(ungated.overallBuy().totalCalls(), gated.overallBuy().totalCalls(),
                label + ": the SELL-only gate must never change BUY call counts");
        assertEquals(ungated.overallSell().totalCalls() - ungated.sellByRegime().ranging().totalCalls(),
                gated.overallSell().totalCalls(),
                label + ": gated SELL total must equal ungated SELL total minus its ranging-regime calls");
    }

    /**
     * E8-F3-S4: pins down the new {@code regimeThreshold}-accepting overload structurally — an
     * extreme-low threshold (every ADX reading clears it) must push every decision point's regime
     * split to trending-only, an extreme-high threshold (no ADX reading clears it) must push every
     * one to ranging-only, and the existing 5-arg overload (no explicit threshold) must be
     * byte-identical to calling the new overload with {@code RegimeClassifier.ADX_TRENDING_THRESHOLD}
     * explicitly.
     */
    @Test
    void regimeThresholdOverload_extremesPushEveryDecisionPointToOneRegimeBucket() {
        assertRegimeThresholdBehavesStructurally("BTCUSDT", "backtest/btcusdt-daily-history.csv");
        assertRegimeThresholdBehavesStructurally("DOGEUSDT", "backtest/dogeusdt-daily-history.csv");
    }

    private void assertRegimeThresholdBehavesStructurally(String label, String fixture) {
        List<Candle> candles = BacktestCandleCsvLoader.load(fixture);

        BacktestReport allTrending = BacktestHarness.run(label, candles, BigDecimal.ZERO);
        assertEquals(0, allTrending.buyByRegime().ranging().totalCalls(),
                label + ": an ADX>=0 threshold must classify every decision point as trending (BUY)");
        assertEquals(0, allTrending.sellByRegime().ranging().totalCalls(),
                label + ": an ADX>=0 threshold must classify every decision point as trending (SELL)");

        BacktestReport allRanging = BacktestHarness.run(label, candles, new BigDecimal("1000"));
        assertEquals(0, allRanging.buyByRegime().trending().totalCalls(),
                label + ": an unreachable ADX threshold must classify every decision point as ranging (BUY)");
        assertEquals(0, allRanging.sellByRegime().trending().totalCalls(),
                label + ": an unreachable ADX threshold must classify every decision point as ranging (SELL)");

        BacktestReport implicitDefault = BacktestHarness.run(label, candles);
        BacktestReport explicitDefault = BacktestHarness.run(label, candles, RegimeClassifier.ADX_TRENDING_THRESHOLD);
        assertEquals(implicitDefault.buyByRegime(), explicitDefault.buyByRegime(),
                label + ": the 5-arg overload must be byte-identical to the new overload called with the global default threshold");
        assertEquals(implicitDefault.sellByRegime(), explicitDefault.sellByRegime(),
                label + ": the 5-arg overload must be byte-identical to the new overload called with the global default threshold");
    }

    private void runAndVerify(String label, String fixture) {
        List<Candle> candles = BacktestCandleCsvLoader.load(fixture);
        BacktestReport report = BacktestHarness.run(label, candles);
        report.printTo(System.out);

        int totalFromCounts = report.callCounts().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(report.totalDecisionPoints(), totalFromCounts,
                "every decision point must land in exactly one SignalRuleId bucket");

        for (SignalRuleId ruleId : SignalRuleId.values()) {
            int expected = report.callCounts().get(ruleId);
            if (ruleId.call() == SignalCall.BUY || ruleId.call() == SignalCall.SELL) {
                DirectionalOutcomeStats stats = report.directionalStats().get(ruleId);
                assertEquals(expected, stats.totalCalls(),
                        ruleId + ": directional stats total must match its call count");
                assertExpectancySignsAreSane(ruleId.name(), stats);
            } else {
                assertEquals(expected, report.holdGateStats().get(ruleId).totalCalls(),
                        ruleId + ": hold-gate stats total must match its call count");
            }
        }
        assertExpectancySignsAreSane("Overall BUY", report.overallBuy());
        assertExpectancySignsAreSane("Overall SELL", report.overallSell());

        for (IndicatorId indicatorId : IndicatorId.values()) {
            assertCheckpointStatsAreSane(indicatorId.name(), report.indicatorStats().get(indicatorId));
        }

        // E8-F3-S2: the regime split is additive over the same per-direction totals, not a
        // resample — every BUY/SELL decision point lands in exactly one of {trending, ranging}
        // for its own direction, on top of already landing in exactly one SignalRuleId bucket.
        assertEquals(report.overallBuy().totalCalls(),
                report.buyByRegime().trending().totalCalls() + report.buyByRegime().ranging().totalCalls(),
                "BUY regime split must partition overallBuy's total calls exactly");
        assertEquals(report.overallSell().totalCalls(),
                report.sellByRegime().trending().totalCalls() + report.sellByRegime().ranging().totalCalls(),
                "SELL regime split must partition overallSell's total calls exactly");
        assertExpectancySignsAreSane("BUY trending", report.buyByRegime().trending());
        assertExpectancySignsAreSane("BUY ranging", report.buyByRegime().ranging());
        assertExpectancySignsAreSane("SELL trending", report.sellByRegime().trending());
        assertExpectancySignsAreSane("SELL ranging", report.sellByRegime().ranging());
    }

    /**
     * E2-F4-S2: avgWinReturnPct/avgLossReturnPct are derived from the same WIN/LOSS
     * classification winRate() already reports (win = signedReturnPct > deadband, loss =
     * signedReturnPct < -deadband), so their sign is a structural invariant, not a value under
     * review — an average of strictly-positive numbers can't come out <= 0, and vice versa.
     *
     * <p>E8-F2-S1 adds: tpHit/slHit/horizonExpired always partition scored() exactly (every
     * scored call has exactly one exit reason), by construction of {@code
     * DirectionalAccumulator.record} tallying exactly one {@code ExitReason} per result.
     */
    private void assertExpectancySignsAreSane(String label, DirectionalOutcomeStats stats) {
        for (Checkpoint checkpoint : Checkpoint.values()) {
            CheckpointStats cp = checkpoint == Checkpoint.MIN ? stats.min()
                    : checkpoint == Checkpoint.MID ? stats.mid() : stats.max();
            assertCheckpointStatsAreSane(label + " " + checkpoint, cp);
        }
    }

    /**
     * E8-F3-S1: the same structural invariants above, factored out so they can be reapplied to
     * {@code BacktestReport.indicatorStats()} — a single {@link CheckpointStats} per indicator
     * (no MIN/MID/MAX split, since a lone indicator's read has no rule-derived hold term to
     * bracket a range with), not just the three checkpoints of a combined rule's {@link
     * DirectionalOutcomeStats}.
     */
    private void assertCheckpointStatsAreSane(String label, CheckpointStats cp) {
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
        assertTrue(cp.avgHoldingDays() >= 0, label + ": avg holding days must never be negative");
        assertTrue(cp.expectancyPctAfterCostsAndFunding() <= cp.expectancyPctAfterCosts(),
                label + ": after-costs-and-funding expectancy must never exceed after-costs expectancy");
    }
}
