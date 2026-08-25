package com.autotrade.dashboard.alert;

import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.risk.KillSwitchEvent;
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
 * A single ops-facing system alert — append-only, no mutable field, same audit-log-style
 * pattern as {@code KillSwitchEvent}/{@code OrderAuditEntry}. Deliberately separate from
 * {@code notification.Notification}, whose schema DB-enforces every row to be ticker-scoped
 * (an order outcome or a watchlist signal change) — neither a kill-switch trip nor a
 * live-signal-drift-decay event has a ticker.
 *
 * <p>Exactly one of {@link #killSwitchEvent} (for {@link SystemAlertType#KILL_SWITCH_ENGAGED})
 * or {@link #ruleTableVersion}/{@link #direction}/{@link #checkpoint}/{@link #driftPct}
 * (for {@link SystemAlertType#SIGNAL_DRIFT_DECAY}) is ever populated — DB-enforced by {@code
 * ck_system_alerts_fields}. The kill-switch case references an existing, already-immutable
 * {@code kill_switch_events} row; the drift-decay case has no persisted source row to
 * reference ({@code LiveSignalDriftService.computeDrift} is ephemeral/recomputed-per-call),
 * so its fields are inlined snapshot values instead.
 */
@Entity
@Table(name = "system_alerts")
public class SystemAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "system_alerts_seq")
    @SequenceGenerator(name = "system_alerts_seq", sequenceName = "system_alerts_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private SystemAlertType alertType;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "kill_switch_event_id")
    private KillSwitchEvent killSwitchEvent;

    @Column(name = "rule_table_version", length = 20)
    private String ruleTableVersion;

    @Column(name = "direction", length = 4)
    private String direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkpoint", length = 3)
    private Checkpoint checkpoint;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "drift_pct", precision = 10, scale = 4)
    private Double driftPct;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SystemAlert() {
        // JPA
    }

    public static SystemAlert forKillSwitchEngaged(KillSwitchEvent event) {
        SystemAlert alert = new SystemAlert();
        alert.alertType = SystemAlertType.KILL_SWITCH_ENGAGED;
        alert.killSwitchEvent = event;
        alert.message = "Kill switch engaged by '" + event.getChangedBy() + "'";
        return alert;
    }

    public static SystemAlert forSignalDriftDecay(String ruleTableVersion, String direction, Checkpoint checkpoint,
                                                    double driftPct) {
        SystemAlert alert = new SystemAlert();
        alert.alertType = SystemAlertType.SIGNAL_DRIFT_DECAY;
        alert.ruleTableVersion = ruleTableVersion;
        alert.direction = direction;
        alert.checkpoint = checkpoint;
        alert.driftPct = driftPct;
        alert.message = "Possible signal decay: ruleTableVersion=" + ruleTableVersion + " direction=" + direction
                + " checkpoint=" + checkpoint + " driftPct=" + driftPct;
        return alert;
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

    public SystemAlertType getAlertType() {
        return alertType;
    }

    public String getMessage() {
        return message;
    }

    public KillSwitchEvent getKillSwitchEvent() {
        return killSwitchEvent;
    }

    public String getRuleTableVersion() {
        return ruleTableVersion;
    }

    public String getDirection() {
        return direction;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }

    public Double getDriftPct() {
        return driftPct;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SystemAlert that)) {
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
        return "SystemAlert{id=" + id + ", alertType=" + alertType + ", createdAt=" + createdAt + "}";
    }
}
