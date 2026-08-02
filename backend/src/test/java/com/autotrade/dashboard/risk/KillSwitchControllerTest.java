package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for the kill switch (E6-F2-S2) — status codes and error bodies, service layer mocked. */
@WebMvcTest(controllers = KillSwitchController.class)
@AutoConfigureMockMvc(addFilters = false)
class KillSwitchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private KillSwitchService killSwitchService;
    @MockitoBean
    private OrderService orderService;

    @Test
    void current_noHistory_returnsClearedWithNullChangedAt() throws Exception {
        when(killSwitchService.currentState()).thenReturn(new KillSwitchResponse(KillSwitchState.CLEARED, null, null));

        mockMvc.perform(get("/api/kill-switch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLEARED"))
                .andExpect(jsonPath("$.changedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "alice")
    void engage_flipsStateThenCancelsOpenOrders_returnsCombinedSummary() throws Exception {
        Instant changedAt = Instant.parse("2026-08-02T00:00:00Z");
        when(killSwitchService.engage("alice"))
                .thenReturn(new KillSwitchResponse(KillSwitchState.ENGAGED, changedAt, "alice"));
        when(orderService.cancelAllOpenOrders())
                .thenReturn(new KillSwitchCancelSummary(2, 1, 1, List.of("client-1: outage")));

        mockMvc.perform(post("/api/kill-switch/engage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.killSwitch.state").value("ENGAGED"))
                .andExpect(jsonPath("$.killSwitch.changedBy").value("alice"))
                .andExpect(jsonPath("$.cancelSummary.attempted").value(2))
                .andExpect(jsonPath("$.cancelSummary.cancelled").value(1))
                .andExpect(jsonPath("$.cancelSummary.failed").value(1));

        verify(killSwitchService).engage("alice");
        verify(orderService).cancelAllOpenOrders();
    }

    @Test
    @WithMockUser(username = "bob")
    void clear_delegatesAndReturns200() throws Exception {
        Instant changedAt = Instant.parse("2026-08-02T00:00:00Z");
        when(killSwitchService.clear("bob"))
                .thenReturn(new KillSwitchResponse(KillSwitchState.CLEARED, changedAt, "bob"));

        mockMvc.perform(post("/api/kill-switch/clear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLEARED"))
                .andExpect(jsonPath("$.changedBy").value("bob"));

        verify(killSwitchService).clear("bob");
    }
}
