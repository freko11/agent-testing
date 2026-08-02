package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for the global paper/live switch (E6-F1-S1) — status codes and error bodies, service layer mocked. */
@WebMvcTest(controllers = TradingModeController.class)
@AutoConfigureMockMvc(addFilters = false)
class TradingModeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TradingModeService tradingModeService;

    @Test
    void current_noHistory_returnsPaperWithNullChangedAt() throws Exception {
        when(tradingModeService.currentState())
                .thenReturn(new TradingModeResponse(TradingMode.PAPER, null, 0, 10, false));

        mockMvc.perform(get("/api/trading-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PAPER"))
                .andExpect(jsonPath("$.changedAt").doesNotExist())
                .andExpect(jsonPath("$.successfulPaperTrades").value(0))
                .andExpect(jsonPath("$.paperTradeThreshold").value(10))
                .andExpect(jsonPath("$.liveModeUnlocked").value(false));
    }

    @Test
    void switchToLive_belowThreshold_returns403() throws Exception {
        when(tradingModeService.switchTo(TradingMode.LIVE))
                .thenThrow(new PaperTradeThresholdNotMetException(4, 10));

        mockMvc.perform(post("/api/trading-mode")
                        .contentType("application/json")
                        .content("{\"mode\":\"LIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PAPER_TRADE_THRESHOLD_NOT_MET"));
    }

    @Test
    void switchToLive_atThreshold_returns200() throws Exception {
        Instant changedAt = Instant.parse("2026-07-30T00:00:00Z");
        when(tradingModeService.switchTo(TradingMode.LIVE))
                .thenReturn(new TradingModeResponse(TradingMode.LIVE, changedAt, 10, 10, true));

        mockMvc.perform(post("/api/trading-mode")
                        .contentType("application/json")
                        .content("{\"mode\":\"LIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("LIVE"));
    }

    @Test
    void switchToPaper_delegatesAndReturns200() throws Exception {
        Instant changedAt = Instant.parse("2026-07-30T00:00:00Z");
        when(tradingModeService.switchTo(TradingMode.PAPER))
                .thenReturn(new TradingModeResponse(TradingMode.PAPER, changedAt, 0, 10, false));

        mockMvc.perform(post("/api/trading-mode")
                        .contentType("application/json")
                        .content("{\"mode\":\"PAPER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PAPER"));

        verify(tradingModeService).switchTo(TradingMode.PAPER);
    }

    @Test
    void switchToNull_returns400() throws Exception {
        mockMvc.perform(post("/api/trading-mode")
                        .contentType("application/json")
                        .content("{\"mode\":null}"))
                .andExpect(status().isBadRequest());
    }
}
