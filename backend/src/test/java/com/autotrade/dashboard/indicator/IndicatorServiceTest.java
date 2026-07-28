package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketClosedException;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.marketdata.PriceHistoryResult;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Proves wiring of the three calculators + snapshot persistence, and that MarketDataService failures propagate unmodified. */
@ExtendWith(MockitoExtension.class)
class IndicatorServiceTest {

    @Mock
    private MarketDataService marketDataService;
    @Mock
    private IndicatorSnapshotRepository indicatorSnapshotRepository;

    private IndicatorService service;

    @BeforeEach
    void setUp() {
        service = new IndicatorService(marketDataService, indicatorSnapshotRepository);
    }

    @Test
    void sufficientCandles_computesAllThreeIndicatorsAndPersistsSnapshot() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = IndicatorTestFixtures.candles40();
        when(marketDataService.getPriceHistory("AAPL", 200))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles));

        IndicatorResponse response = service.computeIndicators("AAPL", 200);

        assertEquals(IndicatorTestFixtures.RSI_14_FULL, response.rsi());
        assertEquals(IndicatorTestFixtures.MACD_LINE_FULL, response.macd().line());
        assertEquals(IndicatorTestFixtures.MACD_SIGNAL_FULL, response.macd().signal());
        assertEquals(IndicatorTestFixtures.MACD_HISTOGRAM_FULL, response.macd().histogram());
        assertEquals(IndicatorTestFixtures.SMA_10_FULL, response.movingAverage().shortMa());
        assertEquals(IndicatorTestFixtures.SMA_30_FULL, response.movingAverage().longMa());
        assertEquals(MovingAverageRelation.SHORT_ABOVE_LONG, response.movingAverage().relation());
        assertEquals(Broker.ALPACA, response.source());

        ArgumentCaptor<IndicatorSnapshot> captor = ArgumentCaptor.forClass(IndicatorSnapshot.class);
        verify(indicatorSnapshotRepository).save(captor.capture());
        IndicatorSnapshot saved = captor.getValue();
        assertEquals(IndicatorTestFixtures.RSI_14_FULL, saved.getRsi());
        assertEquals(IndicatorTestFixtures.MACD_LINE_FULL, saved.getMacdLine());
        assertEquals(IndicatorTestFixtures.SMA_10_FULL, saved.getMaShort());
        assertNull(saved.getVolatility());
        assertNull(saved.getVolume());
        assertNull(saved.getVolumeTrend());
    }

    @Test
    void fewerThan34Candles_throwsInsufficientPriceHistory_withoutPersisting() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = IndicatorTestFixtures.candlesFirst(33);
        when(marketDataService.getPriceHistory("AAPL", 200))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles));

        assertThrows(InsufficientPriceHistoryException.class, () -> service.computeIndicators("AAPL", 200));

        verify(indicatorSnapshotRepository, never()).save(any());
    }

    @Test
    void unregisteredTicker_propagatesWithoutPersisting() {
        when(marketDataService.getPriceHistory(eq("ZZZ"), eq(200)))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        assertThrows(TickerNotRegisteredException.class, () -> service.computeIndicators("ZZZ", 200));

        verify(indicatorSnapshotRepository, never()).save(any());
    }

    @Test
    void marketClosed_propagatesWithoutPersisting() {
        when(marketDataService.getPriceHistory(eq("AAPL"), eq(200)))
                .thenThrow(new MarketClosedException("AAPL"));

        assertThrows(MarketClosedException.class, () -> service.computeIndicators("AAPL", 200));

        verify(indicatorSnapshotRepository, never()).save(any());
    }
}
