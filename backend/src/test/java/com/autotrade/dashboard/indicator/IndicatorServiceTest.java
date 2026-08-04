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
        assertEquals(IndicatorTestFixtures.ATR_PCT_DEGENERATE_FULL, response.volatility());
        assertEquals(IndicatorTestFixtures.VOLUME_DEGENERATE_FULL, response.volume());
        assertEquals(IndicatorTestFixtures.VOLUME_TREND_DEGENERATE_FULL, response.volumeTrend());

        ArgumentCaptor<IndicatorSnapshot> captor = ArgumentCaptor.forClass(IndicatorSnapshot.class);
        verify(indicatorSnapshotRepository).save(captor.capture());
        IndicatorSnapshot saved = captor.getValue();
        assertEquals(IndicatorTestFixtures.RSI_14_FULL, saved.getRsi());
        assertEquals(IndicatorTestFixtures.MACD_LINE_FULL, saved.getMacdLine());
        assertEquals(IndicatorTestFixtures.SMA_10_FULL, saved.getMaShort());
        assertEquals(IndicatorTestFixtures.ATR_PCT_DEGENERATE_FULL, saved.getVolatility());
        assertEquals(IndicatorTestFixtures.VOLUME_DEGENERATE_FULL, saved.getVolume());
        assertEquals(IndicatorTestFixtures.VOLUME_TREND_DEGENERATE_FULL, saved.getVolumeTrend());
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

    @Test
    void chartData_sufficientCandles_returnsOneAlignedPointPerIndexFromMinCandles_withoutPersisting() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = IndicatorTestFixtures.candles40();
        when(marketDataService.getPriceHistory("AAPL", 200))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles));

        ChartDataResponse response = service.getChartData("AAPL", 200);

        assertEquals(candles, response.candles());
        assertEquals(candles.size() - IndicatorService.MIN_CANDLES_FOR_INDICATORS + 1, response.indicators().size());

        ChartIndicatorPoint first = response.indicators().get(0);
        List<Candle> firstWindow = candles.subList(0, IndicatorService.MIN_CANDLES_FOR_INDICATORS);
        assertEquals(candles.get(IndicatorService.MIN_CANDLES_FOR_INDICATORS - 1).timestamp(), first.timestamp());
        assertEquals(RsiCalculator.calculate(firstWindow, RsiCalculator.DEFAULT_PERIOD), first.rsi());
        MovingAverageResult firstMa = MovingAverageCrossoverCalculator.calculate(firstWindow,
                MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);
        assertEquals(firstMa.shortMa(), first.maShort());
        assertEquals(firstMa.longMa(), first.maLong());

        ChartIndicatorPoint last = response.indicators().get(response.indicators().size() - 1);
        assertEquals(candles.get(candles.size() - 1).timestamp(), last.timestamp());
        assertEquals(IndicatorTestFixtures.RSI_14_FULL, last.rsi());
        assertEquals(IndicatorTestFixtures.SMA_10_FULL, last.maShort());
        assertEquals(IndicatorTestFixtures.SMA_30_FULL, last.maLong());

        verify(indicatorSnapshotRepository, never()).save(any());
    }

    @Test
    void chartData_fewerThan34Candles_returnsCandlesWithEmptyIndicatorSeries() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = IndicatorTestFixtures.candlesFirst(33);
        when(marketDataService.getPriceHistory("AAPL", 200))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles));

        ChartDataResponse response = service.getChartData("AAPL", 200);

        assertEquals(33, response.candles().size());
        assertEquals(List.of(), response.indicators());
        verify(indicatorSnapshotRepository, never()).save(any());
    }

    @Test
    void chartData_unregisteredTicker_propagates() {
        when(marketDataService.getPriceHistory(eq("ZZZ"), eq(200)))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        assertThrows(TickerNotRegisteredException.class, () -> service.getChartData("ZZZ", 200));

        verify(indicatorSnapshotRepository, never()).save(any());
    }

    @Test
    void chartData_marketClosed_noPriorFetch_propagates() {
        when(marketDataService.getPriceHistory(eq("AAPL"), eq(200)))
                .thenThrow(new MarketClosedException("AAPL"));

        assertThrows(MarketClosedException.class, () -> service.getChartData("AAPL", 200));

        verify(indicatorSnapshotRepository, never()).save(any());
    }

    @Test
    void chartData_marketClosed_withPriorSuccessfulFetch_returnsStaleCachedResponse() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = IndicatorTestFixtures.candles40();
        when(marketDataService.getPriceHistory(eq("AAPL"), eq(200)))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles))
                .thenThrow(new MarketClosedException("AAPL"));

        ChartDataResponse fresh = service.getChartData("AAPL", 200);
        ChartDataResponse stale = service.getChartData("AAPL", 200);

        assertEquals(false, fresh.stale());
        assertEquals(true, stale.stale());
        assertEquals(fresh.candles(), stale.candles());
        assertEquals(fresh.indicators(), stale.indicators());
        assertEquals(fresh.ticker(), stale.ticker());
    }

    @Test
    void chartData_marketClosed_cacheKeyedCaseInsensitively() {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = IndicatorTestFixtures.candles40();
        when(marketDataService.getPriceHistory(eq("aapl"), eq(200)))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles));
        when(marketDataService.getPriceHistory(eq("AAPL"), eq(200)))
                .thenThrow(new MarketClosedException("AAPL"));

        service.getChartData("aapl", 200);
        ChartDataResponse stale = service.getChartData("AAPL", 200);

        assertEquals(true, stale.stale());
    }
}
