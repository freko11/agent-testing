package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;
import com.autotrade.dashboard.signal.SignalRuleId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F1-S1: sweeps RSI/volatility/volume-trend threshold candidates through
 * {@link BacktestHarness} against the same checked-in BTCUSDT/DOGEUSDT fixtures
 * {@link BacktestHarnessTest} (E2-F4-S1/S2) already uses, printing win rate + expectancy
 * per candidate so a threshold change to {@code SignalRuleEngine} is backed by evidence
 * rather than intuition (docs/CHANGELOG.md's E8-F1-S1 entry records the actual finding).
 *
 * <p>Sweeps one dimension at a time — the other thresholds held at
 * {@link RuleThresholds#DEFAULT} — rather than a full cross-product grid, so an expectancy
 * change can be attributed to the one threshold that moved. RSI's two bounds move together
 * (symmetric around 50), matching how they're actually used as a paired oversold/overbought
 * gate.
 *
 * <p><b>Overfitting caveat (deliberate scope boundary):</b> both fixtures are also this
 * pass's only tuning data, so a candidate that wins here is not yet validated
 * out-of-sample — that is E8-F4-S1's explicit follow-up, not attempted here. Treat any
 * resulting threshold change as provisional pending that story, and prefer the existing
 * baseline over a marginal/noisy-looking improvement, especially where {@code n} (scored
 * call count) is small enough that the "difference" could just be sample noise — the same
 * small-n caution E2-F4-S2's win-rate-vs-expectancy gap already illustrates.
 *
 * <p>Assertions here are structural only, mirroring {@link BacktestHarnessTest} — the
 * printed report is the evidence under review, not a regression target. Read the printed
 * output (rerun via {@code ./mvnw test -Dtest=ThresholdCalibrationTest}) for the actual
 * finding.
 */
class ThresholdCalibrationTest {

    private static final List<Candle> BTCUSDT = BacktestCandleCsvLoader.load("backtest/btcusdt-daily-history.csv");
    private static final List<Candle> DOGEUSDT = BacktestCandleCsvLoader.load("backtest/dogeusdt-daily-history.csv");

    private static final RuleThresholds BASELINE = RuleThresholds.DEFAULT;

    private static final List<SignalRuleId> DIRECTIONAL_RULES = List.of(SignalRuleId.BULLISH_UNANIMOUS,
            SignalRuleId.BULLISH_MAJORITY, SignalRuleId.BEARISH_UNANIMOUS, SignalRuleId.BEARISH_MAJORITY);

    @Test
    void sweepRsiThresholds() {
        sweepDimension("RSI oversold/overbought", List.of(
                candidate("20/80", new BigDecimal("20"), new BigDecimal("80"), BASELINE.volatilityExtreme(), BASELINE.volumeDriedUp()),
                candidate("25/75", new BigDecimal("25"), new BigDecimal("75"), BASELINE.volatilityExtreme(), BASELINE.volumeDriedUp()),
                candidate("30/70 (baseline)", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), BASELINE.volumeDriedUp()),
                candidate("35/65", new BigDecimal("35"), new BigDecimal("65"), BASELINE.volatilityExtreme(), BASELINE.volumeDriedUp())));
    }

    @Test
    void sweepVolatilityExtremeThreshold() {
        sweepDimension("Volatility extreme (ATR%)", List.of(
                candidate("5.0", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), new BigDecimal("5.0"), BASELINE.volumeDriedUp()),
                candidate("6.5", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), new BigDecimal("6.5"), BASELINE.volumeDriedUp()),
                candidate("8.0 (baseline)", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), BASELINE.volumeDriedUp()),
                candidate("10.0", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), new BigDecimal("10.0"), BASELINE.volumeDriedUp()),
                candidate("12.0", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), new BigDecimal("12.0"), BASELINE.volumeDriedUp())));
    }

    @Test
    void sweepVolumeDriedUpThreshold() {
        sweepDimension("Volume dried-up ratio", List.of(
                candidate("0.10", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), new BigDecimal("0.10")),
                candidate("0.15", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), new BigDecimal("0.15")),
                candidate("0.20 (baseline)", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), BASELINE.volumeDriedUp()),
                candidate("0.30", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), new BigDecimal("0.30")),
                candidate("0.40", BASELINE.rsiOversold(), BASELINE.rsiOverbought(), BASELINE.volatilityExtreme(), new BigDecimal("0.40"))));
    }

    private record NamedCandidate(String label, RuleThresholds thresholds) {
    }

    private static NamedCandidate candidate(String label, BigDecimal rsiOversold, BigDecimal rsiOverbought,
                                             BigDecimal volatilityExtreme, BigDecimal volumeDriedUp) {
        return new NamedCandidate(label, new RuleThresholds(rsiOversold, rsiOverbought, volatilityExtreme, volumeDriedUp));
    }

    private void sweepDimension(String dimensionName, List<NamedCandidate> candidates) {
        System.out.printf("%n########## Threshold sweep: %s ##########%n", dimensionName);
        for (NamedCandidate candidate : candidates) {
            runAndPrint("BTCUSDT", BTCUSDT, candidate);
            runAndPrint("DOGEUSDT", DOGEUSDT, candidate);
        }
    }

    private void runAndPrint(String symbol, List<Candle> candles, NamedCandidate candidate) {
        BacktestReport report = BacktestHarness.run(symbol + " [" + candidate.label() + "]", candles, candidate.thresholds());
        printCompact(report);
        assertStructurallySane(report);
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

    private void printCheckpointLine(String rowLabel, DirectionalOutcomeStats stats) {
        if (stats == null || stats.totalCalls() == 0) {
            System.out.printf("%-24s (n=0)%n", rowLabel);
            return;
        }
        System.out.printf("%-24s min %5.1f%%win exp%+6.3f%%(n=%-3d) | mid %5.1f%%win exp%+6.3f%%(n=%-3d) | max %5.1f%%win exp%+6.3f%%(n=%-3d)%n",
                rowLabel,
                stats.min().winRate(), stats.min().expectancyPct(), stats.min().scored(),
                stats.mid().winRate(), stats.mid().expectancyPct(), stats.mid().scored(),
                stats.max().winRate(), stats.max().expectancyPct(), stats.max().scored());
    }

    /** Same structural invariants {@link BacktestHarnessTest} already checks against the
     * baseline (E2-F4-S1/S2): every decision point lands in exactly one bucket, and avg
     * win/loss size signs stay consistent with the WIN/LOSS classification they're derived
     * from — reverified per candidate since a threshold shift changes bucket membership. */
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
        }
    }
}
