package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * A single global paper/live mode change (E6-F1-S1) — append-only, same
 * audit-log-style pattern as {@code SignalCallEntry}/{@code Notification}.
 * The "current" mode is always the latest row by id; see {@link
 * TradingModeService}.
 */
@Entity
@Table(name = "trading_mode_events")
public class TradingModeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trading_mode_events_seq")
    @SequenceGenerator(name = "trading_mode_events_seq", sequenceName = "trading_mode_events_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false, length = 10)
    private TradingMode mode;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected TradingModeEvent() {
        // JPA
    }

    public TradingModeEvent(TradingMode mode) {
        this.mode = mode;
    }

    @PrePersist
    void onCreate() {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public TradingMode getMode() {
        return mode;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TradingModeEvent that)) {
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
        return "TradingModeEvent{id=" + id + ", mode=" + mode + ", changedAt=" + changedAt + "}";
    }
}
