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
        when(tradingModeService.currentState()).thenReturn(new TradingModeResponse(TradingMode.PAPER, null));

        mockMvc.perform(get("/api/trading-mode"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("PAPER"))
                .andExpect(jsonPath("$.changedAt").doesNotExist());
    }

    @Test
    void switchToLive_returns403() throws Exception {
        when(tradingModeService.switchTo(TradingMode.LIVE)).thenThrow(new LiveModeNotYetAvailableException());

        mockMvc.perform(post("/api/trading-mode")
                        .contentType("application/json")
                        .content("{\"mode\":\"LIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("LIVE_MODE_NOT_YET_AVAILABLE"));
    }

    @Test
    void switchToPaper_delegatesAndReturns200() throws Exception {
        Instant changedAt = Instant.parse("2026-07-30T00:00:00Z");
        when(tradingModeService.switchTo(TradingMode.PAPER)).thenReturn(new TradingModeResponse(TradingMode.PAPER, changedAt));

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
