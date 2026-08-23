package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.marketdata.PriceHistoryResult;
import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * E8-F5-S3: {@link WeightedVoteShadowScoringService} reconstructs {@code
 * WeightedVoteRuleEngine.evaluate}'s inputs from a stored {@code IndicatorSnapshot} and buckets
 * the result against what was actually recorded. These fixtures hand-pick RSI/MACD/MA-crossover
 * combinations that isolate each of the four buckets (see the class Javadoc's "why exactly four
 * buckets are exhaustive" section) rather than relying on the real BTCUSDT/DOGEUSDT fixtures —
 * same "engineer the specific case, don't hope a real fixture happens to contain it" precedent
 * {@code LiveSignalDriftServiceTest}/{@code BacktestHarnessTpSlTest} already established.
 */
@ExtendWith(MockitoExtension.class)
class WeightedVoteShadowScoringServiceTest {

    private static final Instant DECISION_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("100");

    @Mock
    private SignalCallEntryRepository signalCallEntryRepository;
    @Mock
    private MarketDataService marketDataService;

    private WeightedVoteShadowScoringService service;

    @BeforeEach
    void setUp() {
        service = new WeightedVoteShadowScoringService(signalCallEntryRepository, marketDataService, 30);
    }

    @Test
    void entriesAreClassifiedIntoTheFourAgreementBuckets() {
        Ticker ticker = ticker("BTCUSDT");
        SignalCallEntry agree = neutralEntry(ticker); // HOLD recorded, HOLD weighted -> AGREE
        SignalCallEntry weightedOnlyBuy = loneMacdEntry(ticker, true); // HOLD recorded, weighted BUY
        SignalCallEntry weightedOnlySell = loneMacdEntry(ticker, false); // HOLD recorded, weighted SELL
        SignalCallEntry downgraded = rsiAndMaWithoutMacdEntry(ticker); // BUY recorded, weighted HOLD
        when(signalCallEntryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(agree, weightedOnlyBuy, weightedOnlySell, downgraded));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, List.of()));

        WeightedVoteShadowReport report = service.computeShadowReport(30);

        assertEquals(30, report.lookbackDays());
        assertEquals(4, report.totalEntriesConsidered());
        assertEquals(0, report.skippedEntries());
        assertEquals(1, report.agreeCount());
        assertEquals(1, report.weightedOnlyBuy().count());
        assertEquals(1, report.weightedOnlySell().count());
        assertEquals(1, report.downgradedByWeighted().count());
        assertEquals(2, report.knownLimitations().size(), "the ADX/regime gap and the current-vs-point-in-time "
                + "threshold gap must both be documented, not silently omitted");
    }

    @Test
    void aReconstructionFailureIsSkippedWithoutAbortingTheRestOfTheBatch() {
        Ticker ticker = ticker("BTCUSDT");
        SignalCallEntry broken = neutralEntry(ticker);
        broken.getIndicatorSnapshot().setRsi(null); // forces an NPE inside WeightedVoteRuleEngine.evaluate
        SignalCallEntry ok = neutralEntry(ticker);
        when(signalCallEntryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(broken, ok));

        WeightedVoteShadowReport report = service.computeShadowReport(30);

        assertEquals(2, report.totalEntriesConsidered());
        assertEquals(1, report.skippedEntries());
        assertEquals(1, report.agreeCount());
    }

    @Test
    void weightedOnlyBuyBucketIsWalkForwardScoredAtTheFixedReferenceHorizon() {
        Ticker ticker = ticker("BTCUSDT");
        SignalCallEntry entry = loneMacdEntry(ticker, true);
        when(signalCallEntryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any())).thenReturn(List.of(entry));
        // Day 1: high 106 >= TP price 105 (5% above the 100 decision close) -- take-profit hit,
        // well within the 5-day fixed reference horizon this bucket is scored at.
        List<Candle> candles = List.of(candle(DECISION_AT.plusSeconds(86400), 106, 99, 104));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, candles));

        WeightedVoteShadowReport report = service.computeShadowReport(30);

        assertEquals(1, report.weightedOnlyBuy().count());
        assertEquals(1, report.weightedOnlyBuy().scoring().scored());
        // win=1 @ +5.00%, no loss -> expectancyPct = 5.00, after 20bps round-trip cost = 4.80.
        assertEquals(4.80, report.weightedOnlyBuy().scoring().expectancyPctAfterCosts(), 1e-6);
    }

    @Test
    void downgradedBucketIsScoredAtItsOwnRecordedHoldTerm() {
        Ticker ticker = ticker("BTCUSDT");
        SignalCallEntry entry = rsiAndMaWithoutMacdEntry(ticker);
        when(signalCallEntryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any())).thenReturn(List.of(entry));
        // Day 1: high 106 >= TP price 105 -- take-profit hit, within MAX_DAYS=5.
        List<Candle> candles = List.of(candle(DECISION_AT.plusSeconds(86400), 106, 99, 104));
        when(marketDataService.getPriceHistory(eq("BTCUSDT"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.BINANCE, candles));

        WeightedVoteShadowReport report = service.computeShadowReport(30);

        assertEquals(1, report.downgradedByWeighted().count());
        assertEquals(1, report.downgradedByWeighted().scoring().totalCalls());
        assertEquals(1, report.downgradedByWeighted().scoring().max().scored());
        assertEquals(4.80, report.downgradedByWeighted().scoring().max().expectancyPctAfterCosts(), 1e-6);
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
        when(signalCallEntryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(loneMacdEntry(badTicker, true), loneMacdEntry(goodTicker, true)));

        WeightedVoteShadowReport report = service.computeShadowReport(30);

        assertEquals(2, report.weightedOnlyBuy().count(), "both entries are still bucketed regardless of market data");
        assertEquals(1, report.weightedOnlyBuy().scoring().scored(), "only the good ticker's entry could be scored");
    }

    private Ticker ticker(String symbol) {
        return new Ticker(symbol, AssetType.CRYPTO, null);
    }

    private Candle candle(Instant timestamp, double high, double low, double close) {
        return new Candle(timestamp, BigDecimal.valueOf(close), BigDecimal.valueOf(high), BigDecimal.valueOf(low),
                BigDecimal.valueOf(close), BigDecimal.ONE);
    }

    private IndicatorSnapshot baseSnapshot(Ticker ticker) {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, DECISION_AT, PRICE, Broker.BINANCE);
        snapshot.setRsi(new BigDecimal("50")); // neutral: not <25, not >75
        snapshot.setVolatility(new BigDecimal("2")); // well under the 8.0 extreme threshold
        snapshot.setVolumeTrend(new BigDecimal("1")); // well over the 0.20 dried-up threshold
        snapshot.setMaShort(new BigDecimal("100")); // EQUAL relation -- neither MA vote fires
        snapshot.setMaLong(new BigDecimal("100"));
        snapshot.setMacdLine(BigDecimal.ZERO);
        snapshot.setMacdSignal(BigDecimal.ZERO);
        snapshot.setMacdHistogram(BigDecimal.ZERO); // neither MACD vote fires
        return snapshot;
    }

    /** RSI/MACD/MA all neutral -- both engines resolve NO_STRONG_SIGNAL (HOLD). AGREE bucket. */
    private SignalCallEntry neutralEntry(Ticker ticker) {
        IndicatorSnapshot snapshot = baseSnapshot(ticker);
        return new SignalCallEntry(ticker, snapshot, SignalRuleId.NO_STRONG_SIGNAL, null);
    }

    /** A lone bullish/bearish MACD vote (RSI/MA neutral): unweighted resolves NO_STRONG_SIGNAL
     * (HOLD, only 1 of 3 votes) but the weighted engine's macdWeight=0.714 alone clears the 0.357
     * majority bar -- WEIGHTED_ONLY_BUY/WEIGHTED_ONLY_SELL depending on {@code bullish}. */
    private SignalCallEntry loneMacdEntry(Ticker ticker, boolean bullish) {
        IndicatorSnapshot snapshot = baseSnapshot(ticker);
        snapshot.setMacdLine(bullish ? BigDecimal.ONE : BigDecimal.ONE.negate());
        snapshot.setMacdSignal(BigDecimal.ZERO);
        snapshot.setMacdHistogram(bullish ? BigDecimal.ONE : BigDecimal.ONE.negate());
        return new SignalCallEntry(ticker, snapshot, SignalRuleId.NO_STRONG_SIGNAL, null);
    }

    /** RSI and MA both bullish, MACD neutral: unweighted resolves BULLISH_MAJORITY (recorded BUY,
     * real hold-term), but the weighted engine's weight lives entirely in MACD (0.714) -- since
     * MACD didn't vote, the weighted sum is 0 and falls back to NO_STRONG_SIGNAL (HOLD).
     * DOWNGRADED_BY_WEIGHTED bucket. */
    private SignalCallEntry rsiAndMaWithoutMacdEntry(Ticker ticker) {
        IndicatorSnapshot snapshot = baseSnapshot(ticker);
        snapshot.setRsi(new BigDecimal("10")); // < 25 -> rsiBullish
        snapshot.setMaShort(new BigDecimal("110")); // SHORT_ABOVE_LONG -> maBullish
        snapshot.setMaLong(new BigDecimal("100"));
        HoldTerm holdTerm = new HoldTerm(1, 5, "1-5 days", "test", "test-v1");
        return new SignalCallEntry(ticker, snapshot, SignalRuleId.BULLISH_MAJORITY, holdTerm);
    }
}
