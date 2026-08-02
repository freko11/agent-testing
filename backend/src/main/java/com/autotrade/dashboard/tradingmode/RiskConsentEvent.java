package com.autotrade.dashboard.tradingmode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A one-time risk-consent acknowledgment gating LIVE mode (E6-F1-S3),
 * independent of the paper-trade threshold (E6-F1-S2) — see {@link
 * TradingModeService}. Same append-only audit-log-style pattern as {@link
 * TradingModeEvent}, though in practice this table holds at most one row
 * since {@code giveRiskConsent()} is idempotent.
 */
@Entity
@Table(name = "risk_consents")
public class RiskConsentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "risk_consents_seq")
    @SequenceGenerator(name = "risk_consents_seq", sequenceName = "risk_consents_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "consented_at", nullable = false, updatable = false)
    private Instant consentedAt;

    @PrePersist
    void onCreate() {
        if (consentedAt == null) {
            consentedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Instant getConsentedAt() {
        return consentedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskConsentEvent that)) {
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
        return "RiskConsentEvent{id=" + id + ", consentedAt=" + consentedAt + "}";
    }
}
