package com.autotrade.dashboard.ticker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for POST /api/tickers — status codes and error bodies per E2-F1-S1's design gate, service layer mocked. */
@WebMvcTest(controllers = TickerController.class)
@AutoConfigureMockMvc(addFilters = false)
class TickerControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TickerService tickerService;

    @Test
    void register_newSymbol_returns201() throws Exception {
        when(tickerService.findRegistered("AAPL")).thenReturn(Optional.empty());
        Ticker ticker = new Ticker("AAPL", AssetType.STOCK, "NASDAQ");
        when(tickerService.resolveOrRegister(eq("AAPL"), eq(AssetType.STOCK), eq("NASDAQ"))).thenReturn(ticker);

        mockMvc.perform(post("/api/tickers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TickerController.RegisterTickerRequest("AAPL", AssetType.STOCK, "NASDAQ"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.assetType").value("STOCK"));
    }

    @Test
    void register_existingSameAssetType_returns200() throws Exception {
        Ticker ticker = new Ticker("MSFT", AssetType.STOCK, "NASDAQ");
        when(tickerService.findRegistered("MSFT")).thenReturn(Optional.of(ticker));
        when(tickerService.resolveOrRegister(eq("MSFT"), eq(AssetType.STOCK), any())).thenReturn(ticker);

        mockMvc.perform(post("/api/tickers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TickerController.RegisterTickerRequest("MSFT", AssetType.STOCK, null))))
                .andExpect(status().isOk());
    }

    @Test
    void register_assetTypeConflict_returns409WithErrorBody() throws Exception {
        when(tickerService.resolveOrRegister(eq("ETHUSDT"), eq(AssetType.STOCK), any()))
                .thenThrow(new TickerAssetTypeConflictException("ETHUSDT", AssetType.CRYPTO, AssetType.STOCK));

        mockMvc.perform(post("/api/tickers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TickerController.RegisterTickerRequest("ETHUSDT", AssetType.STOCK, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ASSET_TYPE_CONFLICT"));
    }

    @Test
    void register_missingAssetType_returns400() throws Exception {
        mockMvc.perform(post("/api/tickers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"AAPL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }
}
