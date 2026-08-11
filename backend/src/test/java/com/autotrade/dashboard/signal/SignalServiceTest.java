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

    /** Above {@link RegimeClassifier#ADX_TRENDING_THRESHOLD} (25) — used by every test not
     * specifically exercising the E8-F3-S3 regime gate, so those tests' outcomes are unaffected by
     * it regardless of ticker asset type or matched rule. */
    private static final BigDecimal TRENDING_ADX = new BigDecimal("30");
    private static final BigDecimal RANGING_ADX = new BigDecimal("20");

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
                new BigDecimal("108.0"), MovingAverageRelation.SHORT_ABOVE_LONG, BigDecimal.ZERO);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"), new BigDecimal("20"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot, TRENDING_ADX));

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
                new BigDecimal("108.0"), MovingAverageRelation.EQUAL, BigDecimal.ZERO);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"), new BigDecimal("50"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot, TRENDING_ADX));

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
                new BigDecimal("110.0"), MovingAverageRelation.EQUAL, BigDecimal.ZERO);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.BINANCE,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("100.00"), new BigDecimal("72"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("SOLUSDT", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot, TRENDING_ADX));

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
                new BigDecimal("110.0"), MovingAverageRelation.EQUAL, BigDecimal.ZERO);
        IndicatorResponse response = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("100.00"), new BigDecimal("72"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot, TRENDING_ADX));

        SignalResponse signalResponse = service.computeSignal("AAPL", 200);

        assertEquals(SignalCall.HOLD, signalResponse.call());
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL.name(), signalResponse.matchedRule());
    }

    /**
     * E8-F3-S3: proves {@code RegimeGatedRuleEngine.applySellGate} is actually wired into the real
     * production call path — {@link SignalService} is not mocked here, only {@code
     * IndicatorService} is, so this exercises the real {@code sellGateAppliesTo}/{@code
     * applySellGate} calls, not a mocked stand-in. A crypto ticker's SELL-unanimous indicators, in
     * a RANGING regime, are suppressed to {@code NO_STRONG_SIGNAL} rather than resolving to
     * {@code BEARISH_UNANIMOUS} as they would without the gate.
     */
    @Test
    void sellCallInRangingRegime_forCryptoTicker_suppressedToNoStrongSignal() {
        SignalResponse signalResponse = computeCryptoSignal(bearishIndicators(), RANGING_ADX);

        assertEquals(SignalCall.HOLD, signalResponse.call());
        assertEquals(SignalRuleId.NO_STRONG_SIGNAL.name(), signalResponse.matchedRule());
    }

    /** Control for {@link #sellCallInRangingRegime_forCryptoTicker_suppressedToNoStrongSignal} —
     * identical indicators, a TRENDING regime instead. Confirms the suppression above is really
     * driven by the regime, not some other difference between the two scenarios. */
    @Test
    void sellCallInTrendingRegime_forCryptoTicker_unaffected() {
        SignalResponse signalResponse = computeCryptoSignal(bearishIndicators(), TRENDING_ADX);

        assertEquals(SignalCall.SELL, signalResponse.call());
        assertEquals(SignalRuleId.BEARISH_UNANIMOUS.name(), signalResponse.matchedRule());
    }

    /** E8-F3-S3: proves BUY calls pass through the gate completely unaffected, even in a RANGING
     * regime — {@code RegimeGatedRuleEngine.applySellGate} only ever touches SELL calls, per
     * E8-F4-S2's finding that only the SELL side's out-of-sample evidence held uniformly. */
    @Test
    void buyCallInRangingRegime_forCryptoTicker_unaffected() {
        SignalResponse signalResponse = computeCryptoSignal(bullishIndicators(), RANGING_ADX);

        assertEquals(SignalCall.BUY, signalResponse.call());
        assertEquals(SignalRuleId.BULLISH_UNANIMOUS.name(), signalResponse.matchedRule());
    }

    /** E8-F3-S3: proves the gate is scoped to crypto tickers only — a stock ticker's SELL call in
     * a RANGING regime is unaffected, since {@code RegimeGatedRuleEngine.sellGateAppliesTo}
     * returns {@code false} for {@code AssetType.STOCK} (zero stock evidence exists for this
     * mechanism). */
    @Test
    void sellCallInRangingRegime_forStockTicker_unaffected() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-02-09T00:00:00Z"),
                new BigDecimal("113.10"), Broker.ALPACA);
        IndicatorResponse response = bearishIndicators().apply(ticker, Broker.ALPACA);

        when(indicatorService.computeForSignal("AAPL", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot, RANGING_ADX));

        SignalResponse signalResponse = service.computeSignal("AAPL", 200);

        assertEquals(SignalCall.SELL, signalResponse.call());
        assertEquals(SignalRuleId.BEARISH_UNANIMOUS.name(), signalResponse.matchedRule());
    }

    private SignalResponse computeCryptoSignal(IndicatorFactory indicators, BigDecimal adx) {
        Ticker ticker = new Ticker("BTCUSDT", AssetType.CRYPTO, null);
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-02-09T00:00:00Z"),
                new BigDecimal("50000.00"), Broker.BINANCE);
        IndicatorResponse response = indicators.apply(ticker, Broker.BINANCE);

        when(indicatorService.computeForSignal("BTCUSDT", 200))
                .thenReturn(new IndicatorService.IndicatorComputation(response, snapshot, adx));

        return service.computeSignal("BTCUSDT", 200);
    }

    @FunctionalInterface
    private interface IndicatorFactory {
        IndicatorResponse apply(Ticker ticker, Broker broker);
    }

    private static IndicatorFactory bullishIndicators() {
        return (ticker, broker) -> {
            MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("1.0"), new BigDecimal("0"));
            MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.0"), 30,
                    new BigDecimal("108.0"), MovingAverageRelation.SHORT_ABOVE_LONG, BigDecimal.ZERO);
            return new IndicatorResponse(TickerSummary.from(ticker), broker, Instant.parse("2026-02-09T00:00:00Z"),
                    new BigDecimal("113.10"), new BigDecimal("20"), macd, ma, new BigDecimal("2.0"),
                    new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));
        };
    }

    private static IndicatorFactory bearishIndicators() {
        return (ticker, broker) -> {
            MacdResult macd = new MacdResult(new BigDecimal("-2.0"), new BigDecimal("-1.0"), new BigDecimal("-1.0"), new BigDecimal("0"));
            MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("108.0"), 30,
                    new BigDecimal("111.0"), MovingAverageRelation.SHORT_BELOW_LONG, BigDecimal.ZERO);
            return new IndicatorResponse(TickerSummary.from(ticker), broker, Instant.parse("2026-02-09T00:00:00Z"),
                    new BigDecimal("113.10"), new BigDecimal("80"), macd, ma, new BigDecimal("2.0"),
                    new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));
        };
    }
}
