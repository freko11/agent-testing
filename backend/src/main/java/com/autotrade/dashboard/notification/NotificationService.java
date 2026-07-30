package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.ticker.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Records in-app notifications (E5-F4-S1) for two independent triggers: an
 * {@code Order}'s status transitioning (hooked from {@code
 * OrderService.applyOutcome}) and a watchlisted ticker's Buy/Sell/Hold call
 * changing (hooked from {@link WatchlistSignalPoller}). Every {@code record*}
 * method swallows and logs its own failures — a notification-recording bug
 * must never fail the order response or abort the poller's batch.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final Map<OrderStatus, NotificationType> ORDER_EVENT_TYPES = Map.of(
            OrderStatus.FILLED, NotificationType.ORDER_FILLED,
            OrderStatus.PARTIALLY_FILLED, NotificationType.ORDER_PARTIALLY_FILLED,
            OrderStatus.REJECTED, NotificationType.ORDER_REJECTED,
            OrderStatus.FAILED, NotificationType.ORDER_FAILED,
            OrderStatus.CANCELLED, NotificationType.ORDER_CANCELLED,
            OrderStatus.PARTIALLY_PROTECTED, NotificationType.ORDER_PARTIALLY_PROTECTED,
            OrderStatus.SUBMISSION_UNKNOWN, NotificationType.ORDER_SUBMISSION_UNKNOWN);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Records an order-outcome notification. Called only on a genuine status
     * transition (the caller already checked {@code previousStatus != status}) —
     * {@code PENDING}/{@code SUBMITTED} have no {@link NotificationType}
     * mapping since {@code applyOutcome} never persists those as a final
     * outcome; such a status is silently skipped rather than thrown, since a
     * notification-recording bug must never surface as an order-submission error.
     */
    public void recordOrderOutcome(Order order, OrderStatus previousStatus) {
        try {
            NotificationType eventType = ORDER_EVENT_TYPES.get(order.getStatus());
            if (eventType == null) {
                return;
            }
            String message = order.getTicker().getSymbol() + " order " + order.getStatus()
                    + (order.getRejectionReason() != null ? ": " + order.getRejectionReason() : "");
            repository.save(Notification.forOrder(order.getTicker(), order, eventType, message));
        } catch (RuntimeException e) {
            log.warn("Failed to record order-outcome notification for order {} (previous status {})",
                    order.getId(), previousStatus, e);
        }
    }

    /**
     * Records a signal-change notification for a watchlisted ticker whose
     * call has genuinely transitioned (the caller — {@link
     * WatchlistSignalPoller} — already excludes the first-ever-poll baseline
     * case and non-call-changing rule-detail transitions).
     */
    public void recordSignalChange(Ticker ticker, SignalCall previousCall, SignalCallEntry current) {
        try {
            String message = ticker.getSymbol() + " signal changed " + previousCall + " -> " + current.getCall()
                    + " (" + current.getMatchedRule() + ")";
            repository.save(Notification.forSignalChange(ticker, current, message));
        } catch (RuntimeException e) {
            log.warn("Failed to record signal-change notification for ticker {}", ticker.getSymbol(), e);
        }
    }

    public List<Notification> list(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }

    public long countUnread() {
        return repository.countByReadAtIsNull();
    }

    public void markRead(Long id) {
        repository.findById(id).ifPresent(notification -> {
            notification.markRead();
            repository.save(notification);
        });
    }

    public void markAllRead() {
        repository.findAllByReadAtIsNull()
                .forEach(notification -> {
                    notification.markRead();
                    repository.save(notification);
                });
    }
}
