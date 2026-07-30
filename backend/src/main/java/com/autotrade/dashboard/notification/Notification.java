package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.ticker.Ticker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * A single in-app notification (E5-F4-S1) — append-only, no unique
 * constraint, same audit-log-style pattern as {@code IndicatorSnapshot}/
 * {@code SignalCallEntry}. Exactly one of {@link #order}/{@link
 * #signalCallEntry} is ever set (DB-enforced by {@code
 * ck_notifications_association}): an order-outcome notification, or a
 * watchlisted ticker's signal-change notification. {@link #readAt} is the
 * one intentionally-mutable field, for the unread/read UI state.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq")
    @SequenceGenerator(name = "notifications_seq", sequenceName = "notifications_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "signal_call_id")
    private SignalCallEntry signalCallEntry;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private NotificationType eventType;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "read_at")
    private Instant readAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // JPA
    }

    public static Notification forOrder(Ticker ticker, Order order, NotificationType eventType, String message) {
        Notification notification = new Notification();
        notification.ticker = ticker;
        notification.order = order;
        notification.eventType = eventType;
        notification.message = message;
        return notification;
    }

    public static Notification forSignalChange(Ticker ticker, SignalCallEntry signalCallEntry, String message) {
        Notification notification = new Notification();
        notification.ticker = ticker;
        notification.signalCallEntry = signalCallEntry;
        notification.eventType = NotificationType.SIGNAL_CHANGED;
        notification.message = message;
        return notification;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public Order getOrder() {
        return order;
    }

    public SignalCallEntry getSignalCallEntry() {
        return signalCallEntry;
    }

    public NotificationType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = Instant.now();
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Notification{id=" + id + ", tickerId=" + (ticker != null ? ticker.getId() : null) +
                ", eventType=" + eventType + ", readAt=" + readAt + "}";
    }
}
