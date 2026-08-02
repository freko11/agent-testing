package com.autotrade.dashboard.order;

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

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable audit trail of an order decision and the signal that triggered it (E6-F3-S1) --
 * one row per {@link Order}, written once by {@code OrderService.submitOrder} at that order's
 * first resolved outcome and never updated afterward, unlike {@code Order} itself (which
 * {@code applyOutcome}/{@code refreshOrder}/{@code cancelAllOpenOrders} keep mutating in place
 * as a real-world order's status resolves further). If a later refresh or cancellation changes
 * an {@code Order}'s status, this row deliberately does not follow -- it freezes the decision
 * made at submission time, not the order's current/live state; {@code Order}/{@code
 * OrderResponse}/CSV export remain the source of truth for that.
 */
@Entity
@Table(name = "order_audit_entries")
public class OrderAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_audit_entries_seq")
    @SequenceGenerator(name = "order_audit_entries_seq", sequenceName = "order_audit_entries_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "signal_call_id", nullable = false)
    private SignalCallEntry signalCallEntry;

    @Column(name = "rule_table_version", nullable = false, length = 20)
    private String ruleTableVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private OrderStatus resultStatus;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "broker_order_id", length = 64)
    private String brokerOrderId;

    @Column(name = "entry_price", precision = 20, scale = 8)
    private BigDecimal entryPrice;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "logged_at", nullable = false, updatable = false)
    private Instant loggedAt;

    protected OrderAuditEntry() {
        // JPA
    }

    public OrderAuditEntry(Order order, SignalCallEntry signalCallEntry, String ruleTableVersion,
                            OrderStatus resultStatus, String rejectionReason, String brokerOrderId,
                            BigDecimal entryPrice) {
        this.order = order;
        this.ticker = order.getTicker();
        this.signalCallEntry = signalCallEntry;
        this.ruleTableVersion = ruleTableVersion;
        this.resultStatus = resultStatus;
        this.rejectionReason = rejectionReason;
        this.brokerOrderId = brokerOrderId;
        this.entryPrice = entryPrice;
    }

    @PrePersist
    void onCreate() {
        if (loggedAt == null) {
            loggedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Ticker getTicker() {
        return ticker;
    }

    public SignalCallEntry getSignalCallEntry() {
        return signalCallEntry;
    }

    public String getRuleTableVersion() {
        return ruleTableVersion;
    }

    public OrderStatus getResultStatus() {
        return resultStatus;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public String getBrokerOrderId() {
        return brokerOrderId;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderAuditEntry that)) {
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
        return "OrderAuditEntry{id=" + id + ", orderId=" + (order != null ? order.getId() : null) +
                ", resultStatus=" + resultStatus + "}";
    }
}
