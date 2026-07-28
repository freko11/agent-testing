package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for GET /api/tickers/{symbol}/price-history — status codes and error bodies, MarketDataService mocked. */
@WebMvcTest(controllers = MarketDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class MarketDataControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private MarketDataService marketDataService;

    @Test
    void priceHistory_registeredTicker_returns200() throws Exception {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        List<Candle> candles = List.of(new Candle(Instant.parse("2026-07-24T04:00:00Z"),
                BigDecimal.valueOf(148.5), BigDecimal.valueOf(151.0), BigDecimal.valueOf(147.75),
                BigDecimal.valueOf(150.25), BigDecimal.valueOf(52341000)));
        when(marketDataService.getPriceHistory(eq("AAPL"), anyInt()))
                .thenReturn(new PriceHistoryResult(ticker, Broker.ALPACA, candles));

        mockMvc.perform(get("/api/tickers/AAPL/price-history").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("ALPACA"))
                .andExpect(jsonPath("$.ticker.symbol").value("AAPL"))
                .andExpect(jsonPath("$.candles[0].close").value(150.25));
    }

    @Test
    void priceHistory_unregisteredTicker_returns404() throws Exception {
        when(marketDataService.getPriceHistory(eq("ZZZ"), anyInt()))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        mockMvc.perform(get("/api/tickers/ZZZ/price-history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }

    @Test
    void priceHistory_limitTooHigh_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/price-history").param("limit", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(marketDataService);
    }

    @Test
    void priceHistory_rateLimited_returns429WithRetryAfterHeader() throws Exception {
        when(marketDataService.getPriceHistory(eq("AAPL"), anyInt()))
                .thenThrow(new MarketDataRateLimitedException(Broker.ALPACA, 30L));

        mockMvc.perform(get("/api/tickers/AAPL/price-history"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "30"))
                .andExpect(jsonPath("$.source").value("ALPACA"));
    }

    @Test
    void priceHistory_providerUnavailable_returns503() throws Exception {
        when(marketDataService.getPriceHistory(eq("AAPL"), anyInt()))
                .thenThrow(new MarketDataUnavailableException(Broker.ALPACA, "boom"));

        mockMvc.perform(get("/api/tickers/AAPL/price-history"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("MARKET_DATA_UNAVAILABLE"));
    }
}
