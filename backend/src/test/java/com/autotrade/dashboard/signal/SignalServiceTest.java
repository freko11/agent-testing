package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.IndicatorService;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.marketdata.MarketClosedException;
import com.autotrade.dashboard.marketdata.TickerSummary;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Proves the rule engine is invoked with the right fields, the persisted SignalCallEntry is correct, and IndicatorService failures propagate unmodified. */
@ExtendWith(MockitoExtension.class)
class SignalServiceTest {

    @Mock
    private IndicatorService indicatorService;
    @Mock
    private SignalCallEntryRepository signalCallEntryRepository;

    private SignalService service;

    @BeforeEach
    void setUp() {
        service = new SignalService(indicatorService, signalCallEntryRepository);
    }

    @Test
    void bullishIndicators_computesBuyCallAndPersistsEntry() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-02-09T00:00:00Z"),
                new BigDecimal("113.10"), Broker.ALPACA);

        MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.0"), 30,
                new BigDecimal("108.0"), MovingAverageRelation.SHORT_ABOVE_LONG);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"), new BigDecimal("20"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot));

        SignalResponse signalResponse = service.computeSignal("AAPL", 200);

        assertEquals(SignalCall.BUY, signalResponse.call());
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS.name(), signalResponse.matchedRule());
        assertEquals(SignalRuleEngine.RULE_TABLE_VERSION, signalResponse.ruleTableVersion());
        assertEquals(response, signalResponse.indicators());
        assertEquals("3-10 days", signalResponse.holdTerm().label());
        assertEquals(HoldTermCalculator.HOLD_TERM_TABLE_VERSION, signalResponse.holdTerm().tableVersion());

        ArgumentCaptor<SignalCallEntry> captor = ArgumentCaptor.forClass(SignalCallEntry.class);
        verify(signalCallEntryRepository).save(captor.capture());
        SignalCallEntry saved = captor.getValue();
        assertEquals(ticker, saved.getTicker());
        assertEquals(snapshot, saved.getIndicatorSnapshot());
        assertEquals(SignalCall.BUY, saved.getCall());
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS, saved.getMatchedRule());
        assertEquals(SignalRuleEngine.RULE_TABLE_VERSION, saved.getRuleTableVersion());
        assertEquals(3, saved.getHoldTermMinDays());
        assertEquals(10, saved.getHoldTermMaxDays());
        assertEquals(HoldTermCalculator.HOLD_TERM_TABLE_VERSION, saved.getHoldTermTableVersion());
    }

    @Test
    void holdCall_persistsNullHoldTerm() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-02-09T00:00:00Z"),
                new BigDecimal("113.10"), Broker.ALPACA);

        MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("0"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.0"), 30,
                new BigDecimal("108.0"), MovingAverageRelation.EQUAL);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"), new BigDecimal("50"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot));

        SignalResponse signalResponse = service.computeSignal("AAPL", 200);

        assertEquals(SignalCall.HOLD, signalResponse.call());
        assertNull(signalResponse.holdTerm());

        ArgumentCaptor<SignalCallEntry> captor = ArgumentCaptor.forClass(SignalCallEntry.class);
        verify(signalCallEntryRepository).save(captor.capture());
        SignalCallEntry saved = captor.getValue();
        assertNull(saved.getHoldTermMinDays());
        assertNull(saved.getHoldTermMaxDays());
        assertNull(saved.getHoldTermTableVersion());
    }

    @Test
    void unregisteredTicker_propagatesWithoutPersisting() {
        when(indicatorService.computeForSignal(eq("ZZZ"), eq(200)))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        assertThrows(TickerNotRegisteredException.class, () -> service.computeSignal("ZZZ", 200));

        verify(signalCallEntryRepository, never()).save(any());
    }

    @Test
    void marketClosed_propagatesWithoutPersisting() {
        when(indicatorService.computeForSignal(eq("AAPL"), eq(200)))
                .thenThrow(new MarketClosedException("AAPL"));

        assertThrows(MarketClosedException.class, () -> service.computeSignal("AAPL", 200));

        verify(signalCallEntryRepository, never()).save(any());
    }

    /**
     * E8-F1-S4: proves {@code PerSymbolRuleThresholds} is actually consulted through the real
     * production call path — {@link SignalService} is not mocked here, only {@code
     * IndicatorService} is, so this exercises the real {@code PerSymbolRuleThresholds.forSymbol}
     * lookup and the real 6-arg {@code SignalRuleEngine.evaluate} overload, not a mocked stand-in.
     * RSI=72 with one other bearish vote (MACD) is the exact boundary this scenario is built
     * around: under the global default (rsiOverbought=75) 72 is not bearish, so only one indicator
     * dissents and the call is {@code NO_STRONG_SIGNAL} ({@link #nonOverriddenSymbol_sameRsi72And
     * OtherwiseIdenticalIndicators_staysNoStrongSignal} proves exactly that for a symbol with no
     * override); under SOLUSDT's own override (rsiOverbought=70) the same RSI=72 crosses into
     * bearish territory, becomes a second dissenting vote, and the call becomes {@code
     * BEARISH_MAJORITY} — a real, observable behavior difference driven only by the ticker symbol.
     */
    @Test
    void solusdtOverride_rsi72BearishOnlyUnderPerSymbolOverride_producesBearishMajority() {
        Ticker ticker = new Ticker("SOLUSDT", AssetType.CRYPTO, null);
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-02-09T00:00:00Z"),
                new BigDecimal("100.00"), Broker.BINANCE);

        MacdResult macd = new MacdResult(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("-1.0"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("110.0"), 30,
                new BigDecimal("110.0"), MovingAverageRelation.EQUAL);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.BINANCE,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("100.00"), new BigDecimal("72"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("SOLUSDT", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot));

        SignalResponse signalResponse = service.computeSignal("SOLUSDT", 200);

        assertEquals(SignalCall.SELL, signalResponse.call());
        assertEquals(SignalRuleId.BEARISH_MAJORITY.name(), signalResponse.matchedRule());
    }

    /** Control for {@link #solusdtOverride_rsi72BearishOnlyUnderPerSymbolOverride_producesBearishMajority}
     * — same RSI=72, same MACD/MA/volatility/volume-trend inputs, a symbol with no
     * {@code PerSymbolRuleThresholds} override. Confirms the difference above is really driven by
     * the per-symbol threshold, not some other accidental difference between the two scenarios. */
    @Test
    void nonOverriddenSymbol_sameRsi72AndOtherwiseIdenticalIndicators_staysNoStrongSignal() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-02-09T00:00:00Z"),
                new BigDecimal("100.00"), Broker.ALPACA);

        MacdResult macd = new MacdResult(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("-1.0"), new BigDecimal("0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("110.0"), 30,
                new BigDecimal("110.0"), MovingAverageRelation.EQUAL);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("100.00"), new BigDecimal("72"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot));

        SignalResponse signalResponse = service.computeSignal("AAPL", 200);

        assertEquals(SignalCall.HOLD, signalResponse.call());
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL.name(), signalResponse.matchedRule());
    }
}
