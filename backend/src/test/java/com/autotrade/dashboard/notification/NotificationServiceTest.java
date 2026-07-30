package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Order-outcome/signal-change notification recording, listing, and read-state (E5-F4-S1). */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(repository);
    }

    private Ticker ticker() {
        Ticker ticker = new Ticker("BTCUSDT", AssetType.CRYPTO, null);
        ReflectionTestUtils.setField(ticker, "id", 1L);
        return ticker;
    }

    private Order order(OrderStatus status, String rejectionReason) {
        BrokerCredential credential = new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "k", "s");
        Order order = new Order(ticker(), credential, Broker.BINANCE, AssetType.CRYPTO, OrderSide.BUY,
                new BigDecimal("1"), new BigDecimal("110"), new BigDecimal("90"), "client-id");
        order.setStatus(status);
        order.setRejectionReason(rejectionReason);
        ReflectionTestUtils.setField(order, "id", 10L);
        return order;
    }

    @Test
    void recordOrderOutcome_filled_savesNotificationWithMessage() {
        Order order = order(OrderStatus.FILLED, null);

        service.recordOrderOutcome(order, OrderStatus.PENDING);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals(NotificationType.ORDER_FILLED, captor.getValue().getEventType());
        assertEquals("BTCUSDT order FILLED", captor.getValue().getMessage());
        assertEquals(order, captor.getValue().getOrder());
        assertNull(captor.getValue().getSignalCallEntry());
    }

    @Test
    void recordOrderOutcome_rejected_includesRejectionReasonInMessage() {
        Order order = order(OrderStatus.REJECTED, "Insufficient margin");

        service.recordOrderOutcome(order, OrderStatus.PENDING);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals(NotificationType.ORDER_REJECTED, captor.getValue().getEventType());
        assertEquals("BTCUSDT order REJECTED: Insufficient margin", captor.getValue().getMessage());
    }

    @Test
    void recordOrderOutcome_partiallyProtected_mapsToOwnType() {
        Order order = order(OrderStatus.PARTIALLY_PROTECTED, "Missing stop-loss leg");

        service.recordOrderOutcome(order, OrderStatus.FILLED);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals(NotificationType.ORDER_PARTIALLY_PROTECTED, captor.getValue().getEventType());
    }

    @Test
    void recordOrderOutcome_repositoryThrows_swallowsException() {
        Order order = order(OrderStatus.FILLED, null);
        doThrow(new RuntimeException("db down")).when(repository).save(any());

        service.recordOrderOutcome(order, OrderStatus.PENDING);
        // No exception propagates — a notification-recording failure must never fail an order response.
    }

    @Test
    void recordSignalChange_savesNotificationWithFormattedMessage() {
        Ticker ticker = ticker();
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-29T00:00:00Z"),
                new BigDecimal("100"), Broker.BINANCE);
        SignalCallEntry entry = new SignalCallEntry(ticker, snapshot, SignalRuleId.BULLISH_MAJORITY, null);

        service.recordSignalChange(ticker, SignalCall.HOLD, entry);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertEquals(NotificationType.SIGNAL_CHANGED, captor.getValue().getEventType());
        assertEquals("BTCUSDT signal changed HOLD -> BUY (BULLISH_MAJORITY)", captor.getValue().getMessage());
        assertEquals(entry, captor.getValue().getSignalCallEntry());
        assertNull(captor.getValue().getOrder());
    }

    @Test
    void list_delegatesToRepositoryWithPageRequest() {
        when(repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20))).thenReturn(List.of());

        List<Notification> result = service.list(20);

        assertNotNull(result);
        verify(repository).findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20));
    }

    @Test
    void countUnread_delegatesToRepository() {
        when(repository.countByReadAtIsNull()).thenReturn(3L);

        assertEquals(3L, service.countUnread());
    }

    @Test
    void markRead_existingUnread_setsReadAtAndSaves() {
        Notification notification = Notification.forOrder(ticker(), order(OrderStatus.FILLED, null),
                NotificationType.ORDER_FILLED, "msg");
        when(repository.findById(5L)).thenReturn(Optional.of(notification));

        service.markRead(5L);

        assertNotNull(notification.getReadAt());
        verify(repository).save(notification);
    }

    @Test
    void markRead_unknownId_isNoOp() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        service.markRead(999L);

        verify(repository, never()).save(any());
    }

    @Test
    void markAllRead_marksEveryUnreadNotification() {
        Notification first = Notification.forOrder(ticker(), order(OrderStatus.FILLED, null),
                NotificationType.ORDER_FILLED, "msg1");
        Notification second = Notification.forOrder(ticker(), order(OrderStatus.REJECTED, "bad"),
                NotificationType.ORDER_REJECTED, "msg2");
        when(repository.findAllByReadAtIsNull()).thenReturn(List.of(first, second));

        service.markAllRead();

        assertTrue(first.getReadAt() != null && second.getReadAt() != null);
        verify(repository).save(first);
        verify(repository).save(second);
    }
}
