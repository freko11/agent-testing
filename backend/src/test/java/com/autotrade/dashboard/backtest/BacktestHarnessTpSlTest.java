package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F2-S1: pins down {@link WalkForwardScorer#findFirstCrossing}/{@link WalkForwardScorer#score}'s
 * day-by-day TP/SL walk-forward exactly, with hand-crafted candles engineered to cross on a
 * specific day — something the two real BTCUSDT/DOGEUSDT fixtures ({@link BacktestHarnessTest})
 * can't offer, since their win rate is evidence under review, not ground truth to assert against.
 *
 * <p>All fixtures decision-index at 0 with a 100 decision-close, so TP/SL prices are always
 * +{@link BacktestConfig#TAKE_PROFIT_PCT}/-{@link BacktestConfig#STOP_LOSS_PCT} away from 100
 * for a BUY (mirrored for a SELL). E8-F5-S1: {@code WalkForwardScorer} takes {@code
 * forwardCandles} (candles strictly after the decision day) rather than {@code (candles,
 * decisionIndex)} — every call below passes {@code candles.subList(1, candles.size())} since
 * every fixture here has its decision day at index 0.
 */
class BacktestHarnessTpSlTest {

    private static final BigDecimal DECISION_CLOSE = new BigDecimal("100");

    @Test
    void buyResolvesByTakeProfitCrossing() {
        List<Candle> candles = candles(
                bar(100, 100, 100),   // decision day (index 0)
                bar(101, 99, 100),    // day 1: neither crosses
                bar(106, 100, 105));  // day 2: high 106 >= TP 105

        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward(candles), 5, DECISION_CLOSE, true);

        assertTrue(crossing.isPresent());
        assertEquals(2, crossing.get().daysForward());
        assertEquals(ExitReason.TP_HIT, crossing.get().exitReason());
        assertEquals(0, BacktestConfig.TAKE_PROFIT_PCT.compareTo(crossing.get().signedReturnPct()));
    }

    @Test
    void buyResolvesByStopLossCrossing() {
        List<Candle> candles = candles(
                bar(100, 100, 100),   // decision day
                bar(101, 99, 100),    // day 1: neither crosses
                bar(103, 98, 100),    // day 2: neither crosses
                bar(100, 96, 97));    // day 3: low 96 <= SL 97

        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward(candles), 5, DECISION_CLOSE, true);

        assertTrue(crossing.isPresent());
        assertEquals(3, crossing.get().daysForward());
        assertEquals(ExitReason.SL_HIT, crossing.get().exitReason());
        assertEquals(0, BacktestConfig.STOP_LOSS_PCT.negate().compareTo(crossing.get().signedReturnPct()));
    }

    @Test
    void buyResolvesBySameDayTieBreakInFavorOfStopLoss() {
        List<Candle> candles = candles(
                bar(100, 100, 100),   // decision day
                bar(106, 96, 100));   // day 1: high 106 >= TP 105 AND low 96 <= SL 97

        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward(candles), 5, DECISION_CLOSE, true);

        assertTrue(crossing.isPresent());
        assertEquals(1, crossing.get().daysForward());
        assertEquals(ExitReason.SL_HIT, crossing.get().exitReason(), "same-day ambiguity must resolve conservatively to stop-loss");
    }

    @Test
    void buyFallsBackToHorizonExpiredWhenNeitherCrossesWithinWindow() {
        List<Candle> candles = candles(
                bar(100, 100, 100),   // decision day
                bar(101, 99, 100),    // day 1
                bar(102, 98, 101),    // day 2
                bar(103, 99, 102));   // day 3, within maxDaysForward=3, close 102 (< TP 105, > SL 97)

        List<Candle> forward = forward(candles);
        Optional<WalkForwardScorer.CrossingEvent> crossing = WalkForwardScorer.findFirstCrossing(forward, 3, DECISION_CLOSE, true);
        assertTrue(crossing.isEmpty());

        Optional<DirectionalScoreResult> result = WalkForwardScorer.score(forward, 3, DECISION_CLOSE, true, crossing);
        assertTrue(result.isPresent());
        assertEquals(ExitReason.HORIZON_EXPIRED, result.get().exitReason());
        assertEquals(DirectionalOutcome.WIN, result.get().outcome(), "2% close-to-close move exceeds the 0.25% deadband");
    }

    @Test
    void sellResolvesByTakeProfitCrossingOnPriceDrop() {
        List<Candle> candles = candles(
                bar(100, 100, 100),   // decision day
                bar(96, 94, 95));     // day 1: low 94 <= TP(sell) 95

        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward(candles), 5, DECISION_CLOSE, false);

        assertTrue(crossing.isPresent());
        assertEquals(1, crossing.get().daysForward());
        assertEquals(ExitReason.TP_HIT, crossing.get().exitReason());
    }

    /**
     * A crossing on day 2 must resolve the MID/MAX checkpoints (day >= 2) but leave a MIN
     * checkpoint bounded to day 1 to fall back to its own fixed-day endpoint scoring — the
     * per-checkpoint-bound behavior E8-F2-S1 chose over one shared result applying to all three.
     */
    @Test
    void earlyCrossingOnlyResolvesCheckpointsAtOrAfterItsDay() {
        List<Candle> candles = candles(
                bar(100, 100, 100),   // decision day
                bar(100, 99, 99.5),   // day 1: neither crosses, close slightly down
                bar(106, 100, 105));  // day 2: TP crossed

        List<Candle> forward = forward(candles);
        Optional<WalkForwardScorer.CrossingEvent> crossing = WalkForwardScorer.findFirstCrossing(forward, 3, DECISION_CLOSE, true);
        assertTrue(crossing.isPresent());
        assertEquals(2, crossing.get().daysForward());

        Optional<DirectionalScoreResult> minResult = WalkForwardScorer.score(forward, 1, DECISION_CLOSE, true, crossing);
        Optional<DirectionalScoreResult> maxResult = WalkForwardScorer.score(forward, 2, DECISION_CLOSE, true, crossing);

        assertTrue(minResult.isPresent());
        assertEquals(ExitReason.HORIZON_EXPIRED, minResult.get().exitReason(), "day-1 checkpoint predates the day-2 crossing");
        assertTrue(maxResult.isPresent());
        assertEquals(ExitReason.TP_HIT, maxResult.get().exitReason(), "day-2 checkpoint is at the crossing day itself");
    }

    /** Every fixture in this test decision-indexes at 0, so the forward slice is always
     * everything after the first candle. */
    private static List<Candle> forward(List<Candle> candles) {
        return candles.subList(1, candles.size());
    }

    private static Candle bar(double high, double low, double close) {
        return new Candle(Instant.EPOCH, BigDecimal.valueOf(close), BigDecimal.valueOf(high), BigDecimal.valueOf(low),
                BigDecimal.valueOf(close), BigDecimal.ONE);
    }

    private static List<Candle> candles(Candle... bars) {
        List<Candle> result = new ArrayList<>();
        Instant timestamp = Instant.EPOCH;
        for (Candle bar : bars) {
            result.add(new Candle(timestamp, bar.open(), bar.high(), bar.low(), bar.close(), bar.volume()));
            timestamp = timestamp.plus(1, ChronoUnit.DAYS);
        }
        return result;
    }
}
