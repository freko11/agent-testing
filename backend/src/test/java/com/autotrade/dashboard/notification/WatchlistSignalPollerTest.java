package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.marketdata.MarketClosedException;
import com.autotrade.dashboard.marketdata.TickerSummary;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
import com.autotrade.dashboard.signal.SignalResponse;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.watchlist.WatchlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The watchlist signal-change half of E5-F4-S1 — the first background/scheduled job in this codebase. */
@ExtendWith(MockitoExtension.class)
class WatchlistSignalPollerTest {

    @Mock
    private WatchlistService watchlistService;
    @Mock
    private SignalService signalService;
    @Mock
    private SignalCallEntryRepository signalCallEntryRepository;
    @Mock
    private NotificationService notificationService;

    private WatchlistSignalPoller poller;

    @BeforeEach
    void setUp() {
        poller = new WatchlistSignalPoller(watchlistService, signalService, signalCallEntryRepository, notificationService);
    }

    private static long nextTickerId = 1L;

    /** A fresh id per call — {@code Ticker.equals} is id-based, so two distinct tickers sharing an id would
     * collide under Mockito's equals-based argument matching in the multi-ticker test below. */
    private Ticker ticker(String symbol) {
        Ticker ticker = new Ticker(symbol, AssetType.CRYPTO, null);
        ReflectionTestUtils.setField(ticker, "id", nextTickerId++);
        return ticker;
    }

    private SignalCallEntry entryWithCall(Ticker ticker, SignalCall call) {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-29T00:00:00Z"),
                new BigDecimal("100"), Broker.BINANCE);
        SignalRuleId rule = call == SignalCall.BUY ? SignalRuleId.BULLISH_MAJORITY
                : call == SignalCall.SELL ? SignalRuleId.BEARISH_MAJORITY : SignalRuleId.CONFLICTING_SIGNALS;
        return new SignalCallEntry(ticker, snapshot, rule, null);
    }

    private SignalService.SignalComputation computationWithCall(Ticker ticker, SignalCall call) {
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-29T00:00:00Z"),
                new BigDecimal("100"), Broker.BINANCE);
        MacdResult macd = new MacdResult(new BigDecimal("1"), new BigDecimal("1"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("100"), 30, new BigDecimal("100"),
                MovingAverageRelation.SHORT_ABOVE_LONG);
        IndicatorResponse indicators = new IndicatorResponse(TickerSummary.from(ticker), Broker.BINANCE,
                Instant.parse("2026-07-29T00:00:00Z"), new BigDecimal("100"), new BigDecimal("50"), macd, ma,
                new BigDecimal("2"), new BigDecimal("1000"), new BigDecimal("1"));
        SignalResponse response = new SignalResponse(TickerSummary.from(ticker), call, "RULE", "rationale", "v1",
                null, indicators);
        return new SignalService.SignalComputation(response, snapshot);
    }

    @Test
    void firstEverPoll_noPreviousEntry_establishesBaselineSilently() {
        Ticker ticker = ticker("BTCUSDT");
        when(watchlistService.listTickers()).thenReturn(List.of(ticker));
        when(signalCallEntryRepository.findTopByTickerOrderByCreatedAtDescIdDesc(ticker)).thenReturn(Optional.empty());
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200)).thenReturn(computationWithCall(ticker, SignalCall.BUY));

        poller.pollWatchlist();

        verify(notificationService, never()).recordSignalChange(any(), any(), any());
    }

    @Test
    void callUnchanged_doesNotNotify() {
        Ticker ticker = ticker("BTCUSDT");
        when(watchlistService.listTickers()).thenReturn(List.of(ticker));
        when(signalCallEntryRepository.findTopByTickerOrderByCreatedAtDescIdDesc(ticker))
                .thenReturn(Optional.of(entryWithCall(ticker, SignalCall.HOLD)));
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200)).thenReturn(computationWithCall(ticker, SignalCall.HOLD));

        poller.pollWatchlist();

        verify(notificationService, never()).recordSignalChange(any(), any(), any());
    }

    @Test
    void callChanged_notifiesWithPreviousAndCurrentCall() {
        Ticker ticker = ticker("BTCUSDT");
        SignalCallEntry previous = entryWithCall(ticker, SignalCall.HOLD);
        SignalCallEntry current = entryWithCall(ticker, SignalCall.BUY);
        when(watchlistService.listTickers()).thenReturn(List.of(ticker));
        when(signalCallEntryRepository.findTopByTickerOrderByCreatedAtDescIdDesc(ticker))
                .thenReturn(Optional.of(previous), Optional.of(current));
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200)).thenReturn(computationWithCall(ticker, SignalCall.BUY));

        poller.pollWatchlist();

        verify(notificationService).recordSignalChange(ticker, SignalCall.HOLD, current);
    }

    @Test
    void oneTickerFails_othersStillPolled() {
        Ticker failing = ticker("AAPL");
        Ticker healthy = ticker("BTCUSDT");
        when(watchlistService.listTickers()).thenReturn(List.of(failing, healthy));
        lenient().when(signalCallEntryRepository.findTopByTickerOrderByCreatedAtDescIdDesc(failing))
                .thenReturn(Optional.of(entryWithCall(failing, SignalCall.HOLD)));
        when(signalService.computeSignalWithProvenance("AAPL", 200)).thenThrow(new MarketClosedException("AAPL"));

        SignalCallEntry previous = entryWithCall(healthy, SignalCall.HOLD);
        SignalCallEntry current = entryWithCall(healthy, SignalCall.SELL);
        when(signalCallEntryRepository.findTopByTickerOrderByCreatedAtDescIdDesc(healthy))
                .thenReturn(Optional.of(previous), Optional.of(current));
        when(signalService.computeSignalWithProvenance("BTCUSDT", 200)).thenReturn(computationWithCall(healthy, SignalCall.SELL));

        poller.pollWatchlist();

        verify(notificationService).recordSignalChange(eq(healthy), eq(SignalCall.HOLD), eq(current));
    }
}
