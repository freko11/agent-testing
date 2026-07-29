package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A deterministic, in-memory {@link BrokerAdapter} for tests — never a
 * Spring bean, never autodiscoverable in any profile. {@code autoFill}
 * controls whether {@link #placeOrder} immediately transitions a submitted
 * order to {@code FILLED} at a placeholder price (not a market-price
 * simulation) or leaves it {@code SUBMITTED} for a test to advance manually
 * via {@link #simulateFill}.
 *
 * <p>The scripting hooks ({@link #rejectNextOrderWith}, {@link
 * #failNextCallWith}, {@link #failNextCallsWith}, {@link #simulateFill})
 * exist so tests for retry/outage handling (E4-F1-S2/S3, see {@link
 * RetryingBrokerAdapter}) and the still-open mock-broker E2E test
 * (E1-F4-S1) have something concrete to script against — this class
 * doesn't itself implement retry or outage logic.
 */
public class MockBrokerAdapter implements BrokerAdapter {

    private final Broker broker;
    private final AssetType assetType;
    private final boolean autoFill;
    private final Clock clock;

    private final Map<String, MockOrderState> orders = new ConcurrentHashMap<>();
    private final Map<String, PositionState> positions = new ConcurrentHashMap<>();
    private final AtomicLong brokerOrderSequence = new AtomicLong(1);

    private volatile String pendingRejectionReason;
    private volatile RuntimeException pendingFailure;
    private final AtomicInteger pendingFailureCount = new AtomicInteger(0);

    public MockBrokerAdapter(Broker broker, AssetType assetType) {
        this(broker, assetType, true, Clock.systemUTC());
    }

    public MockBrokerAdapter(Broker broker, AssetType assetType, boolean autoFill, Clock clock) {
        this.broker = broker;
        this.assetType = assetType;
        this.autoFill = autoFill;
        this.clock = clock;
    }

    @Override
    public AssetType supportedAssetType() {
        return assetType;
    }

    @Override
    public Broker broker() {
        return broker;
    }

    @Override
    public BrokerOrderResult placeOrder(BrokerOrderRequest request, TradingMode mode) {
        maybeThrowScripted();

        MockOrderState existing = orders.get(request.clientOrderId());
        if (existing != null) {
            return existing.toResult(clock.instant());
        }

        String rejectionReason = pendingRejectionReason;
        pendingRejectionReason = null;
        if (rejectionReason != null) {
            MockOrderState rejected = new MockOrderState(
                    request.clientOrderId(), null, request, OrderStatus.REJECTED, null, rejectionReason);
            orders.put(request.clientOrderId(), rejected);
            return rejected.toResult(clock.instant());
        }

        String brokerOrderId = "MOCK-" + brokerOrderSequence.getAndIncrement();
        MockOrderState placed = new MockOrderState(
                request.clientOrderId(), brokerOrderId, request, OrderStatus.SUBMITTED, null, null);
        orders.put(request.clientOrderId(), placed);

        if (autoFill) {
            fill(request.clientOrderId(), placeholderFillPrice(request));
        }

        return orders.get(request.clientOrderId()).toResult(clock.instant());
    }

    @Override
    public Optional<BrokerOrderResult> getOrderStatus(String clientOrderId, TradingMode mode) {
        maybeThrowScripted();
        return Optional.ofNullable(orders.get(clientOrderId)).map(state -> state.toResult(clock.instant()));
    }

    @Override
    public Optional<BrokerPosition> getPosition(String symbol, TradingMode mode) {
        maybeThrowScripted();
        PositionState position = positions.get(symbol);
        if (position == null || position.quantity.signum() == 0) {
            return Optional.empty();
        }
        return Optional.of(new BrokerPosition(
                symbol, position.assetType, position.quantity, position.averageEntryPrice, null, clock.instant()));
    }

    @Override
    public BrokerOrderResult cancelOrder(String clientOrderId, TradingMode mode) {
        maybeThrowScripted();

        MockOrderState existing = orders.get(clientOrderId);
        if (existing == null) {
            return new BrokerOrderResult(clientOrderId, null, OrderStatus.FAILED, null, "Unknown clientOrderId", clock.instant());
        }

        if (existing.status == OrderStatus.SUBMITTED) {
            MockOrderState cancelled = existing.withStatus(OrderStatus.CANCELLED);
            orders.put(clientOrderId, cancelled);
            return cancelled.toResult(clock.instant());
        }

        // FILLED (can't cancel a filled order) or already CANCELLED/REJECTED/FAILED — idempotent no-op.
        return existing.toResult(clock.instant());
    }

    @Override
    public BrokerAccountStatus getAccountStatus(TradingMode mode) {
        maybeThrowScripted();
        List<BrokerAccountStatus.AssetBalance> balances =
                List.of(new BrokerAccountStatus.AssetBalance("USD", new BigDecimal("100000"), BigDecimal.ZERO));
        return new BrokerAccountStatus(
                broker, mode, balances, new BigDecimal("100000"), new BigDecimal("100000"), clock.instant());
    }

    /** Next call to any method throws this instead of executing normally, then resets. */
    public void failNextCallWith(RuntimeException exception) {
        failNextCallsWith(exception, 1);
    }

    /** Next {@code times} calls to any method throw this instead of executing normally, then reset. */
    public void failNextCallsWith(RuntimeException exception, int times) {
        this.pendingFailure = exception;
        this.pendingFailureCount.set(times);
    }

    /** Next {@link #placeOrder} returns REJECTED with this reason instead of the normal path, then resets. */
    public void rejectNextOrderWith(String reason) {
        this.pendingRejectionReason = reason;
    }

    /** Manually transitions a SUBMITTED order to FILLED — for use when {@code autoFill} is false. */
    public void simulateFill(String clientOrderId, BigDecimal fillPrice) {
        fill(clientOrderId, fillPrice);
    }

    private void fill(String clientOrderId, BigDecimal fillPrice) {
        MockOrderState state = orders.get(clientOrderId);
        if (state == null || state.status != OrderStatus.SUBMITTED) {
            return;
        }
        MockOrderState filled = state.withFill(fillPrice);
        orders.put(clientOrderId, filled);
        applyToPosition(filled.request, fillPrice);
    }

    private void applyToPosition(BrokerOrderRequest request, BigDecimal fillPrice) {
        positions.compute(request.symbol(), (symbol, current) -> {
            PositionState state = current == null ? new PositionState(request.assetType(), BigDecimal.ZERO, BigDecimal.ZERO) : current;
            BigDecimal signedQty = request.side() == OrderSide.BUY ? request.quantity() : request.quantity().negate();
            BigDecimal newQuantity = state.quantity.add(signedQty);

            BigDecimal newAverageEntryPrice;
            if (newQuantity.signum() == 0) {
                newAverageEntryPrice = BigDecimal.ZERO;
            } else if (request.side() == OrderSide.BUY) {
                BigDecimal totalCost = state.quantity.multiply(state.averageEntryPrice)
                        .add(request.quantity().multiply(fillPrice));
                newAverageEntryPrice = totalCost.divide(newQuantity, 8, RoundingMode.HALF_UP);
            } else {
                newAverageEntryPrice = state.averageEntryPrice;
            }

            return new PositionState(request.assetType(), newQuantity, newAverageEntryPrice);
        });
    }

    private BigDecimal placeholderFillPrice(BrokerOrderRequest request) {
        if (request.entryOrderType() == EntryOrderType.LIMIT) {
            return request.entryLimitPrice();
        }
        return request.takeProfitPrice().add(request.stopLossPrice()).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
    }

    private void maybeThrowScripted() {
        RuntimeException failure = pendingFailure;
        if (failure == null) {
            return;
        }
        if (pendingFailureCount.decrementAndGet() <= 0) {
            pendingFailure = null;
        }
        throw failure;
    }

    private record PositionState(AssetType assetType, BigDecimal quantity, BigDecimal averageEntryPrice) {
    }

    private record MockOrderState(
            String clientOrderId,
            String brokerOrderId,
            BrokerOrderRequest request,
            OrderStatus status,
            BigDecimal filledPrice,
            String rejectionReason) {

        MockOrderState withStatus(OrderStatus newStatus) {
            return new MockOrderState(clientOrderId, brokerOrderId, request, newStatus, filledPrice, rejectionReason);
        }

        MockOrderState withFill(BigDecimal fillPrice) {
            return new MockOrderState(clientOrderId, brokerOrderId, request, OrderStatus.FILLED, fillPrice, rejectionReason);
        }

        BrokerOrderResult toResult(Instant asOf) {
            return new BrokerOrderResult(clientOrderId, brokerOrderId, status, filledPrice, rejectionReason, asOf);
        }
    }
}
