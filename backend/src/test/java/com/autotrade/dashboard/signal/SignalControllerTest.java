package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for GET /api/tickers/{symbol}/signal — status codes and error bodies, SignalService mocked. */
@WebMvcTest(controllers = SignalController.class)
@AutoConfigureMockMvc(addFilters = false)
class SignalControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private SignalService signalService;

    @Test
    void signal_registeredTicker_returns200() throws Exception {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        MacdResult macd = new MacdResult(new BigDecimal("2.0"), new BigDecimal("1.0"), new BigDecimal("1.0"));
        MovingAverageResult ma = new MovingAverageResult(10, new BigDecimal("111.0"), 30,
                new BigDecimal("108.0"), MovingAverageRelation.SHORT_ABOVE_LONG);
        IndicatorResponse indicators = new IndicatorResponse(TickerSummary.from(ticker), Broker.ALPACA,
                Instant.parse("2026-02-09T00:00:00Z"), new BigDecimal("113.10"), new BigDecimal("25"), macd, ma,
                new BigDecimal("2.0"), new BigDecimal("1000000.0000"), new BigDecimal("1.0000"));
        SignalResponse response = SignalResponse.of(indicators, SignalRuleId.BULLISH_UNANIMOUS);
        when(signalService.computeSignal(eq("AAPL"), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/tickers/AAPL/signal").param("limit", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker.symbol").value("AAPL"))
                .andExpect(jsonPath("$.call").value("BUY"))
                .andExpect(jsonPath("$.matchedRule").value("BULLISH_UNANIMOUS"))
                .andExpect(jsonPath("$.ruleTableVersion").value(SignalRuleEngine.RULE_TABLE_VERSION))
                .andExpect(jsonPath("$.indicators.rsi").value(25));
    }

    @Test
    void signal_limitBelowMinimum_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/signal").param("limit", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(signalService);
    }

    @Test
    void signal_limitAboveMaximum_returns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/tickers/AAPL/signal").param("limit", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));

        verifyNoInteractions(signalService);
    }

    @Test
    void signal_unregisteredTicker_returns404() throws Exception {
        when(signalService.computeSignal(eq("ZZZ"), anyInt()))
                .thenThrow(new TickerNotRegisteredException("ZZZ"));

        mockMvc.perform(get("/api/tickers/ZZZ/signal"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }

    @Test
    void signal_marketClosed_returns409() throws Exception {
        when(signalService.computeSignal(eq("AAPL"), anyInt()))
                .thenThrow(new MarketClosedException("AAPL"));

        mockMvc.perform(get("/api/tickers/AAPL/signal"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("MARKET_CLOSED"));
    }
}
