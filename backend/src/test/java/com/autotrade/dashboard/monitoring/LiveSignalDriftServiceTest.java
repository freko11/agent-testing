package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.marketdata.PriceHistoryResult;
import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderAuditEntry;
import com.autotrade.dashboard.order.OrderAuditEntryRepository;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * E8-F5-S1: {@link LiveSignalDriftService} replays {@code OrderAuditEntry} rows against real
 * forward market data via {@code WalkForwardScorer} (E8-F2-S1, promoted to main scope by this
 * story) — hand-crafted synthetic candles engineer specific TP/SL outcomes, the same "the two
 * real fixtures can't provide ground truth for the crossing algorithm itself" precedent {@code
 * BacktestHarnessTpSlTest} already established.
 */
@ExtendWith(MockitoExtension.class)
class LiveSignalDriftServiceTest {

    private static final Instant DECISION_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final BigDecimal DECISION_CLOSE = new BigDecimal("100");
    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 5;

    @Mock
    private OrderAuditEntryRepository orderAuditEntryRepository;
    @Mock
    private MarketDataService marketDataService;

    private LiveSignalDriftService service;

    @BeforeEach
    void setUp() {
        // lookbackDays=30, minSampleSize=3, decayThresholdPct=0.5 -- deliberately small/simple
        // values for a unit test, unrelated to the real application.properties defaults.
        service = new LiveSignalDriftService(orderAuditEntryRepository, marketDataService, 30, 3, 0.5);
    }

    @Test
    void cleanTakeProfitHitBucketScoresAsAWinAtEveryCheckpoint() {
        Ticker ticker = ticker("BTCUSDT");
        OrderAuditEntry entry = buyAuditEntry(ticker, DECISION_AT);
        when(orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(any(), any()))
                .thenReturn(List.of(entry));
        // Day 1: high 106 >= TP price 105 (5% above the 100 decision close) -- take-profit hit.
        List<Candle> candles = List.of(candle(DECISION_AT.plusSeconds(86400), 106, 99, 104));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, candles));

        SignalDriftReport report = service.computeDrift(30);

        assertEquals(1, report.totalAuditEntriesConsidered());
        assertEquals(1, report.scoredAuditEntries());
        assertEquals(0, report.skippedAuditEntries());
        assertEquals(1, report.versions().size());
        RuleTableVersionDrift version = report.versions().get(0);
        assertTrue(version.hasBaseline(), "the fixture's SignalCallEntry always carries the current RULE_TABLE_VERSION");
        assertEquals(1, version.buy().totalCalls());
        CheckpointDrift maxCheckpoint = checkpointDrift(version.buy(), Checkpoint.MAX);
        assertEquals(1, maxCheckpoint.scored());
        // win=1 @ +5.00%, no loss -> expectancyPct = 5.00, after 20bps round-trip cost = 4.80.
        assertEquals(4.80, maxCheckpoint.liveExpectancyPctAfterCosts(), 1e-6);
        assertFalse(maxCheckpoint.possibleDecay(), "a single win is nowhere near a decay signal");
        // E8-F5-S2: the crossing resolved on day 1, so avgHoldingDays=1 -> funding cost =
        // (3bps/100) * (24/8 periods/day) * 1 day = 0.09pp -> 4.80 - 0.09 = 4.71.
        assertEquals(4.71, maxCheckpoint.liveExpectancyPctAfterCostsAndFunding(), 1e-6);
    }

    @Test
    void cleanStopLossHitBucketScoresAsALossAtEveryCheckpoint() {
        Ticker ticker = ticker("BTCUSDT");
        OrderAuditEntry entry = buyAuditEntry(ticker, DECISION_AT);
        when(orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(any(), any()))
                .thenReturn(List.of(entry));
        // Day 1: low 95 <= SL price 97 (3% below the 100 decision close) -- stop-loss hit.
        List<Candle> candles = List.of(candle(DECISION_AT.plusSeconds(86400), 101, 95, 97));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, candles));

        SignalDriftReport report = service.computeDrift(30);

        RuleTableVersionDrift version = report.versions().get(0);
        CheckpointDrift maxCheckpoint = checkpointDrift(version.buy(), Checkpoint.MAX);
        assertEquals(1, maxCheckpoint.scored());
        // loss=1 @ -3.00%, no win -> expectancyPct = -3.00, after 20bps cost = -3.20.
        assertEquals(-3.20, maxCheckpoint.liveExpectancyPctAfterCosts(), 1e-6);
        // E8-F5-S2: same day-1 crossing as the TP case -> funding cost = 0.09pp -> -3.20 - 0.09 = -3.29.
        assertEquals(-3.29, maxCheckpoint.liveExpectancyPctAfterCostsAndFunding(), 1e-6);
    }

    /** E8-F5-S2's motivating case: a live sample that looks fine on the cost-only figure (a small
     * but real win, no TP/SL crossing so it resolves via {@code HORIZON_EXPIRED} at the full
     * checkpoint horizon) reveals a longer-holding-duration decay once funding is included — the
     * cost-only comparison alone would never surface this. */
    @Test
    void smallWinThatResolvesAtFullHorizonLooksPositiveAfterCostsButNegativeAfterFunding() {
        Ticker ticker = ticker("BTCUSDT");
        OrderAuditEntry entry = buyAuditEntry(ticker, DECISION_AT);
        when(orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(any(), any()))
                .thenReturn(List.of(entry));
        // Five days, none crossing the 5%-TP (105)/3%-SL (97) band -- resolves via HORIZON_EXPIRED
        // at day 5 (this fixture's MAX_DAYS) with a modest +0.50% close, just past the 0.25%
        // WIN/LOSS deadband.
        List<Candle> candles = List.of(
                candle(DECISION_AT.plusSeconds(1L * 86400), 102, 99, 100.20),
                candle(DECISION_AT.plusSeconds(2L * 86400), 102, 99, 100.30),
                candle(DECISION_AT.plusSeconds(3L * 86400), 102, 99, 100.10),
                candle(DECISION_AT.plusSeconds(4L * 86400), 102, 99, 100.40),
                candle(DECISION_AT.plusSeconds(5L * 86400), 102, 99, 100.50));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, candles));

        SignalDriftReport report = service.computeDrift(30);

        RuleTableVersionDrift version = report.versions().get(0);
        CheckpointDrift maxCheckpoint = checkpointDrift(version.buy(), Checkpoint.MAX);
        assertEquals(1, maxCheckpoint.scored());
        // win=1 @ +0.50%, no loss -> expectancyPct = 0.50, after 20bps cost = 0.30 -- looks
        // profitable on the pre-existing cost-only figure.
        assertEquals(0.30, maxCheckpoint.liveExpectancyPctAfterCosts(), 1e-6);
        // Resolved via HORIZON_EXPIRED at the full 5-day checkpoint horizon -> avgHoldingDays=5 ->
        // funding cost = (3bps/100) * (24/8 periods/day) * 5 days = 0.45pp -> 0.30 - 0.45 = -0.15:
        // the funding-adjusted figure flips negative even though the cost-only one still looks fine.
        assertEquals(-0.15, maxCheckpoint.liveExpectancyPctAfterCostsAndFunding(), 1e-6);
    }

    @Test
    void possibleDecayNeverFlaggedBelowTheConfiguredMinimumSampleSize() {
        // minSampleSize is 3 (see setUp); only 2 SL-hit entries -- a resoundingly bad live
        // expectancy, but too small a sample to ever flag decay on its own.
        Ticker ticker = ticker("BTCUSDT");
        List<Candle> slHitCandles = List.of(candle(DECISION_AT.plusSeconds(86400), 101, 95, 97));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, slHitCandles));
        when(orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(any(), any()))
                .thenReturn(List.of(buyAuditEntry(ticker, DECISION_AT), buyAuditEntry(ticker, DECISION_AT)));

        SignalDriftReport report = service.computeDrift(30);

        RuleTableVersionDrift version = report.versions().get(0);
        for (Checkpoint checkpoint : Checkpoint.values()) {
            CheckpointDrift drift = checkpointDrift(version.buy(), checkpoint);
            assertEquals(2, drift.scored());
            assertTrue(drift.driftPct() <= -0.5, "sanity: the raw drift really is bad enough to otherwise qualify");
            assertFalse(drift.possibleDecay(), checkpoint + ": below minimum sample size must never flag decay");
        }
    }

    @Test
    void possibleDecayFlaggedOnceSampleSizeIsMetAndDriftExceedsThreshold() {
        Ticker ticker = ticker("BTCUSDT");
        List<Candle> slHitCandles = List.of(candle(DECISION_AT.plusSeconds(86400), 101, 95, 97));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, slHitCandles));
        when(orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(any(), any()))
                .thenReturn(List.of(buyAuditEntry(ticker, DECISION_AT), buyAuditEntry(ticker, DECISION_AT),
                        buyAuditEntry(ticker, DECISION_AT)));

        SignalDriftReport report = service.computeDrift(30);

        RuleTableVersionDrift version = report.versions().get(0);
        CheckpointDrift maxCheckpoint = checkpointDrift(version.buy(), Checkpoint.MAX);
        assertEquals(3, maxCheckpoint.scored());
        assertTrue(maxCheckpoint.possibleDecay(), "sample size met and drift is well beyond the 0.5pp threshold");
    }

    @Test
    void aPerTickerMarketDataFailureIsSkippedWithoutAbortingOtherTickers() {
        Ticker badTicker = ticker("DEADCOIN");
        Ticker goodTicker = ticker("BTCUSDT");
        when(marketDataService.getPriceHistory(eq("DEADCOIN"), anyInt()))
                .thenThrow(new RuntimeException("simulated market-data outage"));
        List<Candle> tpHitCandles = List.of(candle(DECISION_AT.plusSeconds(86400), 106, 99, 104));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(goodTicker, Broker.BINANCE, tpHitCandles));
        when(orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(any(), any()))
                .thenReturn(List.of(buyAuditEntry(badTicker, DECISION_AT), buyAuditEntry(goodTicker, DECISION_AT)));

        SignalDriftReport report = service.computeDrift(30);

        assertEquals(2, report.totalAuditEntriesConsidered());
        assertEquals(1, report.scoredAuditEntries(), "the bad ticker's entry must be skipped, not counted as scored");
        assertEquals(1, report.skippedAuditEntries());
        // The good ticker's TP-hit entry must still have been accumulated despite the other
        // ticker's failure -- one ticker's market-data outage never aborts the rest of the run.
        RuleTableVersionDrift version = report.versions().get(0);
        assertEquals(1, version.buy().totalCalls());
    }

    private CheckpointDrift checkpointDrift(DirectionalDrift drift, Checkpoint checkpoint) {
        return drift.checkpoints().stream()
                .filter(cp -> cp.checkpoint() == checkpoint)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No CheckpointDrift for " + checkpoint));
    }

    private Ticker ticker(String symbol) {
        return new Ticker(symbol, AssetType.CRYPTO, null);
    }

    private Candle candle(Instant timestamp, double high, double low, double close) {
        return new Candle(timestamp, BigDecimal.valueOf(close), BigDecimal.valueOf(high), BigDecimal.valueOf(low),
                BigDecimal.valueOf(close), BigDecimal.ONE);
    }

    /** A BUY audit entry (SignalRuleId.BULLISH_MAJORITY) whose entry leg filled -- the only
     * {@code resultStatus} shape {@link LiveSignalDriftService} ever rescoures. Its {@link
     * SignalCallEntry} always carries the live {@code SignalRuleEngine.RULE_TABLE_VERSION} (its
     * public constructor derives it internally, with no way to override), so every fixture built
     * this way naturally has {@link LiveDriftBaseline#RULE_TABLE_VERSION} as its version too. */
    private OrderAuditEntry buyAuditEntry(Ticker ticker, Instant decisionAt) {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, decisionAt, DECISION_CLOSE, Broker.BINANCE);
        HoldTerm holdTerm = new HoldTerm(MIN_DAYS, MAX_DAYS, MIN_DAYS + "-" + MAX_DAYS + " days", "test", "test-v1");
        SignalCallEntry signalCallEntry = new SignalCallEntry(ticker, snapshot, SignalRuleId.BULLISH_MAJORITY, holdTerm);
        BrokerCredential credential = new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "enc-key", "enc-secret");
        Order order = new Order(ticker, credential, Broker.BINANCE, ticker.getAssetType(), OrderSide.BUY,
                new BigDecimal("1"), new BigDecimal("105"), new BigDecimal("97"), "client-" + UUID.randomUUID());
        return new OrderAuditEntry(order, signalCallEntry, signalCallEntry.getRuleTableVersion(), OrderStatus.FILLED,
                null, "broker-order-id", DECISION_CLOSE);
    }
}
