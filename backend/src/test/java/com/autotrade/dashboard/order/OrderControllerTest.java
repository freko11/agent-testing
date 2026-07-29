package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for POST /api/tickers/{symbol}/orders (E5-F2-S1) — status codes and error bodies, service layer mocked. */
@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private OrderService orderService;

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(new PlaceOrderRequest(new BigDecimal("100"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90")));
    }

    @Test
    void placeOrder_filled_returns201() throws Exception {
        TradeOrderResponse response = new TradeOrderResponse(1L, "client-id", "broker-order-1", Broker.BINANCE,
                OrderSide.BUY, new BigDecimal("1.00000000"), OrderStatus.FILLED, new BigDecimal("100.5"), null,
                Instant.parse("2026-07-29T00:00:00Z"));
        when(orderService.submitOrder(org.mockito.ArgumentMatchers.eq("BTCUSDT"),
                org.mockito.ArgumentMatchers.any(PlaceOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tickers/BTCUSDT/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FILLED"))
                .andExpect(jsonPath("$.brokerOrderId").value("broker-order-1"));
    }

    @Test
    void placeOrder_holdSignal_returns409SignalNotActionable() throws Exception {
        when(orderService.submitOrder(org.mockito.ArgumentMatchers.eq("BTCUSDT"),
                org.mockito.ArgumentMatchers.any(PlaceOrderRequest.class)))
                .thenThrow(new SignalNotActionableException("BTCUSDT", "CONFLICTING_SIGNALS"));

        mockMvc.perform(post("/api/tickers/BTCUSDT/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("SIGNAL_NOT_ACTIONABLE"));
    }

    @Test
    void placeOrder_invalidTradeRequest_returns400() throws Exception {
        when(orderService.submitOrder(org.mockito.ArgumentMatchers.eq("BTCUSDT"),
                org.mockito.ArgumentMatchers.any(PlaceOrderRequest.class)))
                .thenThrow(new InvalidTradeRequestException("Leverage must be between 1x and 20x."));

        mockMvc.perform(post("/api/tickers/BTCUSDT/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void placeOrder_noBrokerCredential_returns503() throws Exception {
        when(orderService.submitOrder(org.mockito.ArgumentMatchers.eq("BTCUSDT"),
                org.mockito.ArgumentMatchers.any(PlaceOrderRequest.class)))
                .thenThrow(new BrokerCredentialNotConfiguredException(Broker.BINANCE,
                        com.autotrade.dashboard.common.TradingMode.PAPER));

        mockMvc.perform(post("/api/tickers/BTCUSDT/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("BROKER_CREDENTIAL_NOT_CONFIGURED"));
    }

    @Test
    void placeOrder_negativeAmount_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(new PlaceOrderRequest(new BigDecimal("-100"), BigDecimal.ONE,
                new BigDecimal("110"), new BigDecimal("90")));

        mockMvc.perform(post("/api/tickers/BTCUSDT/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void placeOrder_unregisteredTicker_returns404() throws Exception {
        when(orderService.submitOrder(org.mockito.ArgumentMatchers.eq("NOPE"),
                org.mockito.ArgumentMatchers.any(PlaceOrderRequest.class)))
                .thenThrow(new com.autotrade.dashboard.ticker.TickerNotRegisteredException("NOPE"));

        mockMvc.perform(post("/api/tickers/NOPE/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TICKER_NOT_REGISTERED"));
    }
}
