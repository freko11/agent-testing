package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketClosedException;
import com.autotrade.dashboard.marketdata.TickerSummary;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for GET /api/tickers/{symbol}/indicators — status codes and error bodies, IndicatorService mocked. */
@WebMvcTest(controllers = IndicatorController.class)
@AutoConfigureMockMvc(addFilters = false)
class IndicatorControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private IndicatorService indicatorService;

    @Test
    void indicators_registeredTicker_returns200() throws Exception {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        MacdResult macd = new MacdResult(new BigDecimal("2.11694333"), new BigDecimal("2.13097767"), new BigDecimal("-0.01403434"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.92000000"), 30,
                new BigDecimal("108.94000000"), MovingAverageRelation.SHORT_ABOVE_LONG);
        IndicatorResponse response = IndicatorResponse.from(ticker, Broker.ALPACA,
                new com.autotrade.dashboard.marketdata.Candle(Instant.parse("2026-02-09T00:00:00Z"),
                        new BigDecimal("113.10"), new BigDecimal("113.10"), new BigDecimal("113.10"),
                        new BigDecimal("113.10"), BigDecimal.valueOf(1_000_000)),
                new IndicatorService.BigDecimalIndicators(new BigDecimal("77.8751"), macd, ma,
                        new BigDecimal("0.4842"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000")));
        when(indicatorService.computeIndicators(eq("AAPL"), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/tickers/AAPL/indicators").param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker.symbol").value("AAPL"))
                .andExpect(jsonPath("$.source").value("ALPACA"))
                .andExpect(jsonPath("$.rsi").value(77.8751))
                .andExpect(jsonPath("$.macd.line").value(2.11694333))
                .andExpect(jsonPath("$.movingAverage.relation").value("SHORT_ABOVE_LONG"))
                .andExpect(jsonPath("$.volatility").value(0.4842))
                .andExpect(jsonPath("$.volume").value(1000000.0000))
                .andExpect(jsonPath("$.volumeTrend").value(1.0000));
    }

    @Test
    void indicators_limitBelowMinimum_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/indicators").param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(indicatorService);
    }

    @Test
    void indicators_limitAboveMaximum_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/indicators").param("limit", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(indicatorService);
    }

    @Test
    void indicators_unregisteredTicker_returns404() throws Exception {
        when(indicatorService.computeIndicators(eq("ZZZ"), anyInt()))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        mockMvc.perform(get("/api/tickers/ZZZ/indicators"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }

    @Test
    void indicators_marketClosed_returns409() throws Exception {
        when(indicatorService.computeIndicators(eq("AAPL"), anyInt()))
                .thenThrow(new MarketClosedException("AAPL"));

        mockMvc.perform(get("/api/tickers/AAPL/indicators"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MARKET_CLOSED"));
    }

    @Test
    void indicators_insufficientPriceHistory_returns422() throws Exception {
        when(indicatorService.computeIndicators(eq("NEWCO"), anyInt()))
                .thenThrow(new InsufficientPriceHistoryException("\"NEWCO\" has only 5 candle(s); at least 34 are required."));

        mockMvc.perform(get("/api/tickers/NEWCO/indicators"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_PRICE_HISTORY"));
    }

    @Test
    void chartData_registeredTicker_returns200WithCandlesAndIndicators() throws Exception {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        Candle candle = new Candle(Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"),
                new BigDecimal("113.10"), new BigDecimal("113.10"), new BigDecimal("113.10"), BigDecimal.valueOf(1_000_000));
        ChartIndicatorPoint point = new ChartIndicatorPoint(candle.timestamp(), new BigDecimal("77.8751"),
                new BigDecimal("111.92000000"), new BigDecimal("108.94000000"));
        ChartDataResponse response = new ChartDataResponse(TickerSummary.from(ticker), Broker.ALPACA,
                List.of(candle), List.of(point));
        when(indicatorService.getChartData(eq("AAPL"), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/tickers/AAPL/chart-data").param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker.symbol").value("AAPL"))
                .andExpect(jsonPath("$.source").value("ALPACA"))
                .andExpect(jsonPath("$.candles[0].close").value(113.10))
                .andExpect(jsonPath("$.indicators[0].rsi").value(77.8751))
                .andExpect(jsonPath("$.indicators[0].maShort").value(111.92000000))
                .andExpect(jsonPath("$.indicators[0].maLong").value(108.94000000));
    }

    @Test
    void chartData_limitBelowMinimum_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/chart-data").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(indicatorService);
    }

    @Test
    void chartData_limitAboveMaximum_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/chart-data").param("limit", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(indicatorService);
    }

    @Test
    void chartData_unregisteredTicker_returns404() throws Exception {
        when(indicatorService.getChartData(eq("ZZZ"), anyInt()))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        mockMvc.perform(get("/api/tickers/ZZZ/chart-data"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }

    @Test
    void chartData_marketClosed_returns409() throws Exception {
        when(indicatorService.getChartData(eq("AAPL"), anyInt()))
                .thenThrow(new MarketClosedException("AAPL"));

        mockMvc.perform(get("/api/tickers/AAPL/chart-data"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MARKET_CLOSED"));
    }
}
