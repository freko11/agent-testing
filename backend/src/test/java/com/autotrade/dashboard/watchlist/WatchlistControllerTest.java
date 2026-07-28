package com.autotrade.dashboard.watchlist;

import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for /api/watchlist (E3-F3-S1) — status codes and error bodies, service layer mocked. */
@WebMvcTest(controllers = WatchlistController.class)
@AutoConfigureMockMvc(addFilters = false)
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private WatchlistService watchlistService;

    @Test
    void list_returnsSavedEntries() throws Exception {
        Ticker ticker = new Ticker("BTCUSDT", AssetType.CRYPTO, null);
        WatchlistEntry entry = new WatchlistEntry(ticker);
        when(watchlistService.list()).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker.symbol").value("BTCUSDT"));
    }

    @Test
    void add_newTicker_returns201() throws Exception {
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        WatchlistEntry entry = new WatchlistEntry(ticker);
        when(watchlistService.contains("AAPL")).thenReturn(false);
        when(watchlistService.add("AAPL")).thenReturn(entry);

        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistController.AddWatchlistEntryRequest("AAPL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker.symbol").value("AAPL"));
    }

    @Test
    void add_alreadyWatchlisted_returns200() throws Exception {
        Ticker ticker = new Ticker("MSFT", AssetType.STOCK, "NASDAQ");
        WatchlistEntry entry = new WatchlistEntry(ticker);
        when(watchlistService.contains("MSFT")).thenReturn(true);
        when(watchlistService.add("MSFT")).thenReturn(entry);

        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistController.AddWatchlistEntryRequest("MSFT"))))
                .andExpect(status().isOk());
    }

    @Test
    void add_unregisteredTicker_returns404() throws Exception {
        when(watchlistService.contains("NOPE")).thenThrow(new TickerNotRegisteredException("NOPE"));

        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WatchlistController.AddWatchlistEntryRequest("NOPE"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }

    @Test
    void add_blankSymbol_returns400() throws Exception {
        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void remove_watchlistedTicker_returns204() throws Exception {
        doNothing().when(watchlistService).remove("BTCUSDT");

        mockMvc.perform(delete("/api/watchlist/BTCUSDT"))
                .andExpect(status().isNoContent());
    }

    @Test
    void remove_unregisteredTicker_returns404() throws Exception {
        doThrow(new TickerNotRegisteredException("NOPE")).when(watchlistService).remove("NOPE");

        mockMvc.perform(delete("/api/watchlist/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }
}
