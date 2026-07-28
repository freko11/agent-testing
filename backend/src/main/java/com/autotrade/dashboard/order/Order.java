package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.ticker.AssetType;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single broker order (bracket entry + TP + SL). {@code clientOrderId} is
 * an app-generated idempotency key created before any broker call — never
 * regenerate it on retry, or a retried submission can duplicate an
 * already-placed order (E4.1). No {@code ON DELETE CASCADE} on any FK here;
 * order history should never be silently orphaned or cascade-deleted.
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 1)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "id", precision = 19, scale = 0)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    /** Nullable now; populated once E5 exists to link an order back to the signal that triggered it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "indicator_snapshot_id")
    private IndicatorSnapshot indicatorSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JdbcTypeCode(SqlTypes.NUMERIC)
    @JoinColumn(name = "broker_credential_id", nullable = false)
    private BrokerCredential brokerCredential;

    /** Denormalized snapshot of which broker this order routed through, independent of credential rotation. */
    @Enumerated(EnumType.STRING)
    @Column(name = "broker", nullable = false, length = 20)
    private Broker broker;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_mode", nullable = false, length = 10)
    private TradingMode orderMode = TradingMode.PAPER;

    /** Denormalized from the ticker at order-creation time. */
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 10)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 10)
    private OrderSide side;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "requested_amount_usd", precision = 20, scale = 8)
    private BigDecimal requestedAmountUsd;

    @Column(name = "leverage", nullable = false, precision = 5, scale = 2)
    private BigDecimal leverage = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_order_type", nullable = false, length = 10)
    private EntryOrderType entryOrderType = EntryOrderType.MARKET;

    @Column(name = "entry_price", precision = 20, scale = 8)
    private BigDecimal entryPrice;

    @Column(name = "take_profit_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal takeProfitPrice;

    @Column(name = "stop_loss_price", nullable = false, precision = 20, scale = 8)
    private BigDecimal stopLossPrice;

    /** App-generated idempotency key, created before any broker call — see class Javadoc. */
    @Column(name = "client_order_id", nullable = false, unique = true, length = 64)
    private String clientOrderId;

    @Column(name = "broker_order_id", length = 64)
    private String brokerOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "submitted_at")
    private Instant submittedAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "filled_at")
    private Instant filledAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Order() {
        // JPA
    }

    public Order(Ticker ticker, BrokerCredential brokerCredential, Broker broker, AssetType assetType,
                 OrderSide side, BigDecimal quantity, BigDecimal takeProfitPrice, BigDecimal stopLossPrice,
                 String clientOrderId) {
        this.ticker = ticker;
        this.brokerCredential = brokerCredential;
        this.broker = broker;
        this.assetType = assetType;
        this.side = side;
        this.quantity = quantity;
        this.takeProfitPrice = takeProfitPrice;
        this.stopLossPrice = stopLossPrice;
        this.clientOrderId = clientOrderId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public IndicatorSnapshot getIndicatorSnapshot() {
        return indicatorSnapshot;
    }

    public void setIndicatorSnapshot(IndicatorSnapshot indicatorSnapshot) {
        this.indicatorSnapshot = indicatorSnapshot;
    }

    public BrokerCredential getBrokerCredential() {
        return brokerCredential;
    }

    public void setBrokerCredential(BrokerCredential brokerCredential) {
        this.brokerCredential = brokerCredential;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public TradingMode getOrderMode() {
        return orderMode;
    }

    public void setOrderMode(TradingMode orderMode) {
        this.orderMode = orderMode;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(AssetType assetType) {
        this.assetType = assetType;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getRequestedAmountUsd() {
        return requestedAmountUsd;
    }

    public void setRequestedAmountUsd(BigDecimal requestedAmountUsd) {
        this.requestedAmountUsd = requestedAmountUsd;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    public EntryOrderType getEntryOrderType() {
        return entryOrderType;
    }

    public void setEntryOrderType(EntryOrderType entryOrderType) {
        this.entryOrderType = entryOrderType;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getTakeProfitPrice() {
        return takeProfitPrice;
    }

    public void setTakeProfitPrice(BigDecimal takeProfitPrice) {
        this.takeProfitPrice = takeProfitPrice;
    }

    public BigDecimal getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(BigDecimal stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public String getBrokerOrderId() {
        return brokerOrderId;
    }

    public void setBrokerOrderId(String brokerOrderId) {
        this.brokerOrderId = brokerOrderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(Instant filledAt) {
        this.filledAt = filledAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Order order)) {
            return false;
        }
        return id != null && id.equals(order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Order{id=" + id + ", clientOrderId='" + clientOrderId + "', broker=" + broker +
                ", orderMode=" + orderMode + ", side=" + side + ", status=" + status + "}";
    }
}
