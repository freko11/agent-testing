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

        MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("1.0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.0"), 30,
                new BigDecimal("108.0"), MovingAverageRelation.SHORT_ABOVE_LONG);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"), new BigDecimal("25"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot));

        SignalResponse signalResponse = service.computeSignal("AAPL", 200);

        assertEquals(SignalCall.BUY, signalResponse.call());
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS.name(), signalResponse.matchedRule());
        assertEquals(SignalRuleEngine.RULE_TABLE_VERSION, signalResponse.ruleTableVersion());
        assertEquals(response, signalResponse.indicators());

        ArgumentCaptor<SignalCallEntry> captor = ArgumentCaptor.forClass(SignalCallEntry.class);
        verify(signalCallEntryRepository).save(captor.capture());
        SignalCallEntry saved = captor.getValue();
        assertEquals(ticker, saved.getTicker());
        assertEquals(snapshot, saved.getIndicatorSnapshot());
        assertEquals(SignalCall.BUY, saved.getCall());
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS, saved.getMatchedRule());
        assertEquals(SignalRuleEngine.RULE_TABLE_VERSION, saved.getRuleTableVersion());
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
}
