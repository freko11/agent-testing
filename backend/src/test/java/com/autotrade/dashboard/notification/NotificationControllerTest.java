package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contract test for the in-app notification list (E5-F4-S1) — status codes and error bodies, service layer mocked. */
@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private NotificationService notificationService;

    private Notification sampleNotification() {
        Ticker ticker = new Ticker("BTCUSDT", AssetType.CRYPTO, null);
        ReflectionTestUtils.setField(ticker, "id", 1L);
        BrokerCredential credential = new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "k", "s");
        Order order = new Order(ticker, credential, Broker.BINANCE, AssetType.CRYPTO, OrderSide.BUY,
                new BigDecimal("1"), new BigDecimal("110"), new BigDecimal("90"), "client-id");
        order.setStatus(OrderStatus.FILLED);
        Notification notification = Notification.forOrder(ticker, order, NotificationType.ORDER_FILLED, "BTCUSDT order FILLED");
        ReflectionTestUtils.setField(notification, "id", 1L);
        return notification;
    }

    @Test
    void list_defaultLimit_returns200() throws Exception {
        when(notificationService.list(50)).thenReturn(List.of(sampleNotification()));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tickerSymbol").value("BTCUSDT"))
                .andExpect(jsonPath("$[0].eventType").value("ORDER_FILLED"));
    }

    @Test
    void list_limitTooHigh_returns400() throws Exception {
        mockMvc.perform(get("/api/notifications").param("limit", "501"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void list_limitTooLow_returns400() throws Exception {
        mockMvc.perform(get("/api/notifications").param("limit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    @Test
    void unreadCount_returnsCount() throws Exception {
        when(notificationService.countUnread()).thenReturn(4L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4));
    }

    @Test
    void markRead_returns204() throws Exception {
        mockMvc.perform(post("/api/notifications/1/read"))
                .andExpect(status().isNoContent());

        verify(notificationService).markRead(1L);
    }

    @Test
    void markAllRead_returns204() throws Exception {
        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllRead();
    }
}
