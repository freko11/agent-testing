package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for GET /api/orders and POST /api/orders/{id}/refresh (E5-F3-S1) — status codes and error bodies, service layer mocked. */
@WebMvcTest(controllers = OrderQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OrderService orderService;

    private OrderResponse sampleResponse(Long id, OrderStatus status, String rejectionReason) {
        return new OrderResponse(id, "BTCUSDT", AssetType.CRYPTO, Broker.BINANCE, TradingMode.PAPER, OrderSide.BUY,
                new BigDecimal("1.00000000"), new BigDecimal("100"), BigDecimal.ONE, EntryOrderType.MARKET,
                new BigDecimal("110"), new BigDecimal("90"), new BigDecimal("100.5"), "client-id", "broker-order-1",
                status, rejectionReason, Instant.parse("2026-07-29T00:00:00Z"), Instant.parse("2026-07-29T00:00:01Z"),
                Instant.parse("2026-07-29T00:00:00Z"), Instant.parse("2026-07-29T00:00:01Z"));
    }

    @Test
    void listOrders_defaultLimit_returns200() throws Exception {
        when(orderService.listOrders(50)).thenReturn(List.of(sampleResponse(1L, OrderStatus.FILLED, null)));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("FILLED"));
    }

    @Test
    void listOrders_limitTooHigh_returns400() throws Exception {
        mockMvc.perform(get("/api/orders").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void listOrders_limitTooLow_returns400() throws Exception {
        mockMvc.perform(get("/api/orders").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void refreshOrder_success_returns200() throws Exception {
        when(orderService.refreshOrder(eq(7L))).thenReturn(sampleResponse(7L, OrderStatus.FILLED, null));

        mockMvc.perform(post("/api/orders/7/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FILLED"));
    }

    @Test
    void refreshOrder_unknownId_returns404() throws Exception {
        when(orderService.refreshOrder(eq(404L))).thenThrow(new OrderNotFoundException(404L));

        mockMvc.perform(post("/api/orders/404/refresh"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
    }

    @Test
    void refreshOrder_brokerUnavailable_returns503() throws Exception {
        when(orderService.refreshOrder(eq(9L)))
                .thenThrow(new OrderRefreshUnavailableException("Could not refresh order status from BINANCE: down"));

        mockMvc.perform(post("/api/orders/9/refresh"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("ORDER_REFRESH_UNAVAILABLE"));
    }

    @Test
    void exportOrders_success_returnsCsvWithAttachmentHeader() throws Exception {
        when(orderService.exportOrdersCsv(eq(LocalDate.parse("2026-07-01")), eq(LocalDate.parse("2026-07-10")), isNull()))
                .thenReturn("Order ID,Created At (UTC)\r\n1,2026-07-05T00:00:00Z\r\n");

        mockMvc.perform(get("/api/orders/export").param("start", "2026-07-01").param("end", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"trade-history-2026-07-01-to-2026-07-10.csv\""))
                .andExpect(content().string("Order ID,Created At (UTC)\r\n1,2026-07-05T00:00:00Z\r\n"));
    }

    @Test
    void exportOrders_withMode_passesModeThrough() throws Exception {
        when(orderService.exportOrdersCsv(eq(LocalDate.parse("2026-07-01")), eq(LocalDate.parse("2026-07-10")), eq(TradingMode.PAPER)))
                .thenReturn("Order ID,Created At (UTC)\r\n");

        mockMvc.perform(get("/api/orders/export")
                        .param("start", "2026-07-01").param("end", "2026-07-10").param("mode", "PAPER"))
                .andExpect(status().isOk());
    }

    @Test
    void exportOrders_startAfterEnd_returns400() throws Exception {
        when(orderService.exportOrdersCsv(eq(LocalDate.parse("2026-07-10")), eq(LocalDate.parse("2026-07-01")), isNull()))
                .thenThrow(new InvalidTradeRequestException("start date must not be after end date."));

        mockMvc.perform(get("/api/orders/export").param("start", "2026-07-10").param("end", "2026-07-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }
}
