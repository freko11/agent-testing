package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.IndicatorSnapshot;
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
 * A single point-in-time Buy/Sell/Hold call produced by {@link SignalRuleEngine}, append-only
 * (no unique constraint on ticker+day — same audit-log-style pattern as
 * {@link IndicatorSnapshot}). Named "Entry" (not "SignalCall") to avoid colliding with the
 * {@link SignalCall} enum it carries.
 */
@Entity
@Table(name = "signal_calls")
public class SignalCallEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "signal_calls_seq")
    @SequenceGenerator(name = "signal_calls_seq", sequenceName = "signal_calls_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "indicator_snapshot_id", nullable = false)
    private IndicatorSnapshot indicatorSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "call", nullable = false, length = 10)
    private SignalCall call;

    @Column(name = "rule_table_version", nullable = false, length = 20)
    private String ruleTableVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "matched_rule", nullable = false, length = 30)
    private SignalRuleId matchedRule;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SignalCallEntry() {
        // JPA
    }

    public SignalCallEntry(Ticker ticker, IndicatorSnapshot indicatorSnapshot, SignalRuleId matchedRule) {
        this.ticker = ticker;
        this.indicatorSnapshot = indicatorSnapshot;
        this.matchedRule = matchedRule;
        this.call = matchedRule.call();
        this.ruleTableVersion = SignalRuleEngine.RULE_TABLE_VERSION;
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

    public IndicatorSnapshot getIndicatorSnapshot() {
        return indicatorSnapshot;
    }

    public SignalCall getCall() {
        return call;
    }

    public String getRuleTableVersion() {
        return ruleTableVersion;
    }

    public SignalRuleId getMatchedRule() {
        return matchedRule;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SignalCallEntry that)) {
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
        return "SignalCallEntry{id=" + id + ", tickerId=" + (ticker != null ? ticker.getId() : null) +
                ", call=" + call + ", matchedRule=" + matchedRule + ", ruleTableVersion='" + ruleTableVersion + "'}";
    }
}
