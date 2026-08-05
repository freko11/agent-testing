package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleId;
import org.junit.jupiter.api.Test;

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
