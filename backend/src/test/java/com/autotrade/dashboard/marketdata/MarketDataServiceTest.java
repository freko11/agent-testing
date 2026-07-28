package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import com.autotrade.dashboard.ticker.TickerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Proves asset-type routing (STOCK->Alpaca, CRYPTO->Binance) and the not-registered short-circuit, with no HTTP involved. */
@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private TickerService tickerService;
    @Mock
    private MarketDataClient alpacaClient;
    @Mock
    private MarketDataClient binanceClient;

    private MarketDataService service;

    @BeforeEach
    void setUp() {
        lenient().when(alpacaClient.supportedAssetType()).thenReturn(AssetType.STOCK);
        lenient().when(alpacaClient.broker()).thenReturn(Broker.ALPACA);
        lenient().when(binanceClient.supportedAssetType()).thenReturn(AssetType.CRYPTO);
        lenient().when(binanceClient.broker()).thenReturn(Broker.BINANCE);
        service = new MarketDataService(tickerService, List.of(alpacaClient, binanceClient));
    }

    @Test
    void stockTicker_routesToAlpaca() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        when(tickerService.findRegistered("AAPL")).thenReturn(Optional.of(ticker));
        List<Candle> candles = List.of(new Candle(Instant.now(), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE));
        when(alpacaClient.fetchRecentCandles(eq("AAPL"), eq(50))).thenReturn(candles);

        PriceHistoryResult result = service.getPriceHistory("AAPL", 50);

        assertEquals(Broker.ALPACA, result.source());
        assertEquals(candles, result.candles());
        verify(binanceClient, never()).fetchRecentCandles(any(), anyInt());
    }

    @Test
    void cryptoTicker_routesToBinance() {
        Ticker ticker = new Ticker("BTCUSDT", AssetType.CRYPTO, null);
        when(tickerService.findRegistered("BTCUSDT")).thenReturn(Optional.of(ticker));
        List<Candle> candles = List.of(new Candle(Instant.now(), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
                BigDecimal.TEN, BigDecimal.TEN));
        when(binanceClient.fetchRecentCandles(eq("BTCUSDT"), eq(50))).thenReturn(candles);

        PriceHistoryResult result = service.getPriceHistory("BTCUSDT", 50);

        assertEquals(Broker.BINANCE, result.source());
        assertEquals(candles, result.candles());
        verify(alpacaClient, never()).fetchRecentCandles(any(), anyInt());
    }

    @Test
    void unregisteredTicker_throwsWithoutCallingAnyClient() {
        when(tickerService.findRegistered("ZZZ")).thenReturn(Optional.empty());

        assertThrows(TickerNotRegisteredException.class, () -> service.getPriceHistory("ZZZ", 50));

        verify(alpacaClient, never()).fetchRecentCandles(any(), anyInt());
        verify(binanceClient, never()).fetchRecentCandles(any(), anyInt());
    }
}
