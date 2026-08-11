package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Day-by-day TP/SL-aware walk-forward scoring (E8-F2-S1), promoted to main scope (E8-F5-S1) from
 * {@code BacktestHarness}'s package-private {@code score}/{@code findFirstCrossing}/{@code
 * percentChange} methods — the same pure primitive, now reused by two callers instead of one:
 * {@code backtest.BacktestHarness} (fixture replay, {@code src/test/java}) and {@code
 * monitoring.LiveSignalDriftService} (real forward market data fetched after a live {@code
 * OrderAuditEntry}'s decision point, {@code src/main/java}).
 *
 * <p><b>Signature change from the original test-scope version</b>: {@code findFirstCrossing}/
 * {@code score} used to take {@code (List<Candle> candles, int decisionIndex, ...)}, indexing
 * into one contiguous fixture series anchored at index 0. That shape assumes a single candle list
 * spans both the decision day and its forward history, which a live audit-log replay doesn't have
 * — the decision day's candle isn't necessarily even fetched, only the forward candles are. These
 * methods instead take {@code forwardCandles}: candles strictly AFTER the decision day, index 0 =
 * day+1. {@code BacktestHarness} adapts by passing {@code candles.subList(decisionIndex + 1,
 * candles.size())}.
 *
 * <p>A single daily OHLC bar can't say whether the high or low happened first intraday — if both
 * TP and SL cross on the same day, stop-loss wins (the conservative assumption).
 */
public final class WalkForwardScorer {

    private WalkForwardScorer() {
    }

    /** One decision point's shared TP/SL scan result: how many days forward it resolved, which
     * side crossed first, and the signed return (in call-direction terms) that side represents. */
    public record CrossingEvent(int daysForward, ExitReason exitReason, BigDecimal signedReturnPct) {
    }

    /**
     * @param crossing this decision point's shared TP/SL scan result (E8-F2-S1), if any — applied
     *                  to this checkpoint only when it occurred at or before {@code daysForward},
     *                  so an early crossing doesn't collapse MIN/MID/MAX to identical results.
     * @return empty if {@code daysForward} candles past the decision point aren't available in
     *         {@code forwardCandles} and no TP/SL crossing within {@code daysForward} resolved it
     *         first.
     */
    public static Optional<DirectionalScoreResult> score(List<Candle> forwardCandles, int daysForward,
                                                           BigDecimal decisionClose, boolean isBuy,
                                                           Optional<CrossingEvent> crossing) {
        if (crossing.isPresent() && crossing.get().daysForward() <= daysForward) {
            CrossingEvent event = crossing.get();
            DirectionalOutcome outcome = event.exitReason() == ExitReason.TP_HIT ? DirectionalOutcome.WIN : DirectionalOutcome.LOSS;
            return Optional.of(new DirectionalScoreResult(outcome, event.signedReturnPct(), event.exitReason(), event.daysForward()));
        }

        int forwardIndex = daysForward - 1;
        if (forwardIndex < 0 || forwardIndex >= forwardCandles.size()) {
            return Optional.empty();
        }
        BigDecimal pctChange = percentChange(decisionClose, forwardCandles.get(forwardIndex).close());
        BigDecimal signedForCall = isBuy ? pctChange : pctChange.negate();

        DirectionalOutcome outcome = signedForCall.abs().compareTo(BacktestConfig.WIN_LOSS_DEADBAND_PCT) <= 0
                ? DirectionalOutcome.WASH
                : (signedForCall.signum() > 0 ? DirectionalOutcome.WIN : DirectionalOutcome.LOSS);
        return Optional.of(new DirectionalScoreResult(outcome, signedForCall, ExitReason.HORIZON_EXPIRED, daysForward));
    }

    /**
     * Day-by-day walk-forward scan (E8-F2-S1) for the first day, within {@code maxDaysForward} of
     * the decision point, whose high/low crosses the take-profit or stop-loss price implied by
     * {@link BacktestConfig#TAKE_PROFIT_PCT}/{@link BacktestConfig#STOP_LOSS_PCT}. Runs once per
     * decision point and is then applied to each {@link Checkpoint} independently by {@link
     * #score}, bounded by that checkpoint's own day count.
     */
    public static Optional<CrossingEvent> findFirstCrossing(List<Candle> forwardCandles, int maxDaysForward,
                                                              BigDecimal decisionClose, boolean isBuy) {
        return findFirstCrossing(forwardCandles, maxDaysForward, decisionClose, isBuy,
                BacktestConfig.TAKE_PROFIT_PCT, BacktestConfig.STOP_LOSS_PCT);
    }

    /**
     * As the 4-arg overload above, but takes an explicit {@code takeProfitPct}/{@code
     * stopLossPct} instead of always reading {@link BacktestConfig}'s fixed constants (E8-F3-S5) —
     * lets {@code BacktestHarness.runIndicatorExpectancy} replay the per-indicator scan at an
     * alternate horizon/TP-SL combination without touching the production diagnostic defaults the
     * 4-arg overload (and every existing caller) still uses.
     */
    public static Optional<CrossingEvent> findFirstCrossing(List<Candle> forwardCandles, int maxDaysForward,
                                                              BigDecimal decisionClose, boolean isBuy,
                                                              BigDecimal takeProfitPct, BigDecimal stopLossPct) {
        BigDecimal tpDistance = decisionClose.multiply(takeProfitPct)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal slDistance = decisionClose.multiply(stopLossPct)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal tpPrice = isBuy ? decisionClose.add(tpDistance) : decisionClose.subtract(tpDistance);
        BigDecimal slPrice = isBuy ? decisionClose.subtract(slDistance) : decisionClose.add(slDistance);

        int lastDay = Math.min(maxDaysForward, forwardCandles.size());
        for (int day = 1; day <= lastDay; day++) {
            Candle candle = forwardCandles.get(day - 1);
            boolean slHit = isBuy ? candle.low().compareTo(slPrice) <= 0 : candle.high().compareTo(slPrice) >= 0;
            if (slHit) {
                return Optional.of(new CrossingEvent(day, ExitReason.SL_HIT, stopLossPct.negate()));
            }
            boolean tpHit = isBuy ? candle.high().compareTo(tpPrice) >= 0 : candle.low().compareTo(tpPrice) <= 0;
            if (tpHit) {
                return Optional.of(new CrossingEvent(day, ExitReason.TP_HIT, takeProfitPct));
            }
        }
        return Optional.empty();
    }

    public static BigDecimal percentChange(BigDecimal from, BigDecimal to) {
        return to.subtract(from).divide(from, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }
}
