package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.broker.Broker;
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
 * A single point-in-time snapshot of indicator *inputs* for a ticker — RSI,
 * MACD components, moving averages, volatility/volume — with no Buy/Sell/Hold
 * call or rule-table version. That derivation belongs to a future, additive
 * {@code signal_calls} table in E2-F3.
 */
@Entity
@Table(name = "indicator_snapshots")
public class IndicatorSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "indicator_snapshots_seq")
    @SequenceGenerator(name = "indicator_snapshots_seq", sequenceName = "indicator_snapshots_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "price", nullable = false, precision = 20, scale = 8)
    private BigDecimal price;

    @Column(name = "rsi", precision = 9, scale = 4)
    private BigDecimal rsi;

    @Column(name = "macd_line", precision = 20, scale = 8)
    private BigDecimal macdLine;

    @Column(name = "macd_signal", precision = 20, scale = 8)
    private BigDecimal macdSignal;

    @Column(name = "macd_histogram", precision = 20, scale = 8)
    private BigDecimal macdHistogram;

    @Column(name = "ma_short", precision = 20, scale = 8)
    private BigDecimal maShort;

    @Column(name = "ma_long", precision = 20, scale = 8)
    private BigDecimal maLong;

    @Column(name = "volatility", precision = 9, scale = 4)
    private BigDecimal volatility;

    @Column(name = "volume", precision = 20, scale = 4)
    private BigDecimal volume;

    @Column(name = "volume_trend", precision = 9, scale = 4)
    private BigDecimal volumeTrend;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_data_source", nullable = false, length = 20)
    private Broker marketDataSource;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IndicatorSnapshot() {
        // JPA
    }

    public IndicatorSnapshot(Ticker ticker, Instant snapshotAt, BigDecimal price, Broker marketDataSource) {
        this.ticker = ticker;
        this.snapshotAt = snapshotAt;
        this.price = price;
        this.marketDataSource = marketDataSource;
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

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
    }

    public Instant getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(Instant snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRsi() {
        return rsi;
    }

    public void setRsi(BigDecimal rsi) {
        this.rsi = rsi;
    }

    public BigDecimal getMacdLine() {
        return macdLine;
    }

    public void setMacdLine(BigDecimal macdLine) {
        this.macdLine = macdLine;
    }

    public BigDecimal getMacdSignal() {
        return macdSignal;
    }

    public void setMacdSignal(BigDecimal macdSignal) {
        this.macdSignal = macdSignal;
    }

    public BigDecimal getMacdHistogram() {
        return macdHistogram;
    }

    public void setMacdHistogram(BigDecimal macdHistogram) {
        this.macdHistogram = macdHistogram;
    }

    public BigDecimal getMaShort() {
        return maShort;
    }

    public void setMaShort(BigDecimal maShort) {
        this.maShort = maShort;
    }

    public BigDecimal getMaLong() {
        return maLong;
    }

    public void setMaLong(BigDecimal maLong) {
        this.maLong = maLong;
    }

    public BigDecimal getVolatility() {
        return volatility;
    }

    public void setVolatility(BigDecimal volatility) {
        this.volatility = volatility;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getVolumeTrend() {
        return volumeTrend;
    }

    public void setVolumeTrend(BigDecimal volumeTrend) {
        this.volumeTrend = volumeTrend;
    }

    public Broker getMarketDataSource() {
        return marketDataSource;
    }

    public void setMarketDataSource(Broker marketDataSource) {
        this.marketDataSource = marketDataSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndicatorSnapshot that)) {
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
        return "IndicatorSnapshot{id=" + id + ", tickerId=" + (ticker != null ? ticker.getId() : null) +
                ", snapshotAt=" + snapshotAt + ", price=" + price + ", marketDataSource=" + marketDataSource + "}";
    }
}
