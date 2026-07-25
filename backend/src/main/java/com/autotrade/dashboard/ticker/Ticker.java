package com.autotrade.dashboard.ticker;

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

@Entity
@Table(name = "tickers")
public class Ticker {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tickers_seq")
    @SequenceGenerator(name = "tickers_seq", sequenceName = "tickers_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @Column(name = "symbol", nullable = false, unique = true, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    private AssetType assetType;

    @Column(name = "exchange", length = 20)
    private String exchange;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Ticker() {
        // JPA
    }

    public Ticker(String symbol, AssetType assetType, String exchange) {
        this.symbol = symbol;
        this.assetType = assetType;
        this.exchange = exchange;
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

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticker ticker)) {
            return false;
        }
        return id != null && id.equals(ticker.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Ticker{id=" + id + ", symbol='" + symbol + "', assetType=" + assetType +
                ", exchange='" + exchange + "'}";
    }
}
