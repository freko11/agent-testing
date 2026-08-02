package com.autotrade.dashboard.risk;

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
 * A single kill-switch engage/clear event (E6-F2-S2) — append-only, same
 * "latest row = current state" pattern as {@code TradingModeEvent}. {@code
 * changedBy} is the session username (nullable — only unpopulated for rows
 * written before an authenticated context existed, which in practice never
 * happens since every mutating endpoint requires session auth).
 */
@Entity
@Table(name = "kill_switch_events")
public class KillSwitchEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "kill_switch_events_seq")
    @SequenceGenerator(name = "kill_switch_events_seq", sequenceName = "kill_switch_events_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kill_switch_state", nullable = false, length = 10)
    private KillSwitchState state;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    protected KillSwitchEvent() {
        // JPA
    }

    public KillSwitchEvent(KillSwitchState state, String changedBy) {
        this.state = state;
        this.changedBy = changedBy;
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

    public KillSwitchState getState() {
        return state;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KillSwitchEvent that)) {
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
        return "KillSwitchEvent{id=" + id + ", state=" + state + ", changedAt=" + changedAt
                + ", changedBy='" + changedBy + "'}";
    }
}
