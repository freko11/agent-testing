package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.brokeradapter.BrokerAdapter;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterAmbiguousOrderException;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterException;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterRateLimitedException;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterRouter;
import com.autotrade.dashboard.brokeradapter.BrokerAdapterUnavailableException;
import com.autotrade.dashboard.brokeradapter.BrokerOrderRequest;
import com.autotrade.dashboard.brokeradapter.BrokerOrderResult;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalResponse;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Turns a validated "click Trade" request into a real bracket order
 * (E5-F2-S1). Always recomputes the signal server-side rather than trusting
 * whatever {@link SignalResponse} the frontend has cached — a stale call or
 * price is a real risk for a leveraged bracket order. {@code TradingMode} is
 * hardcoded {@link TradingMode#PAPER} here, never taken from the request:
 * {@code LIVE} isn't seeded by either credential bootstrap yet (E4-F2-S1/
 * E4-F3-S1), so live trading is structurally unreachable through this
 * service until E6 builds its consent gate.
 *
 * <p>{@code clientOrderId} is generated once and the {@code Order} row is
 * persisted as {@code PENDING} <em>before</em> the broker is ever called, in
 * its own short-lived write — so the idempotency key survives an app crash
 * mid-call. The broker call itself runs with no open transaction (an
 * external HTTP round-trip, including {@code RetryingBrokerAdapter}'s
 * multi-attempt backoff, must never hold a DB connection). The same row is
 * then finalized in a second short write. Every outcome — filled, a business
 * rejection, a partially-protected fill, or an infrastructure failure — is
 * written onto that row as a normal value; only pre-flight failures (bad
 * ticker/request, no actionable signal, no credential configured) skip
 * creating an {@code Order} row at all.
 *
 * <p>Also serves order status/history (E5-F3-S1): {@link #listOrders} reads
 * straight from the DB (every order already reflects its final synchronous
 * outcome from {@code submitOrder}, so there's no background "still pending"
 * state to poll for) and {@link #refreshOrder} is a manual, explicit
 * per-order re-poll of the broker — no scheduled/background polling exists,
 * matching this codebase's bias against automated background action on
 * money-moving state (E5-F2-S2's explicit confirm step, E4-F3-S2's
 * no-auto-flatten decision).
 */
@Service
public class OrderService {

    private static final int SIGNAL_LIMIT = 200;

    /** Mirrors {@code BinanceFuturesTradingAdapter.MAX_LEVERAGE} and the frontend's {@code validation.ts} — an
     * accepted, flagged triple duplication of the same crypto leverage ceiling across FE/BE-adapter/BE-service. */
    public static final int MAX_CRYPTO_LEVERAGE = 20;

    private final SignalService signalService;
    private final BrokerAdapterRouter router;
    private final BrokerCredentialService brokerCredentialService;
    private final OrderRepository orderRepository;

    public OrderService(SignalService signalService, BrokerAdapterRouter router,
                         BrokerCredentialService brokerCredentialService, OrderRepository orderRepository) {
        this.signalService = signalService;
        this.router = router;
        this.brokerCredentialService = brokerCredentialService;
        this.orderRepository = orderRepository;
    }

    public TradeOrderResponse submitOrder(String symbol, PlaceOrderRequest request) {
        SignalService.SignalComputation computation = signalService.computeSignalWithProvenance(symbol, SIGNAL_LIMIT);
        SignalResponse signal = computation.response();

        if (signal.call() == SignalCall.HOLD) {
            throw new SignalNotActionableException(symbol, signal.matchedRule());
        }

        Ticker ticker = computation.snapshot().getTicker();
        BigDecimal price = signal.indicators().price();
        OrderSide side = signal.call() == SignalCall.BUY ? OrderSide.BUY : OrderSide.SELL;

        validate(request, ticker.getAssetType(), side, price);

        BigDecimal quantity = request.amountUsd().divide(price, 8, RoundingMode.DOWN);
        if (quantity.signum() <= 0) {
            throw new InvalidTradeRequestException(
                    "Requested amount is too small to produce a positive quantity at the current price (" + price + ").");
        }

        BrokerAdapter adapter = router.forAssetType(ticker.getAssetType());
        Broker broker = adapter.broker();
        BrokerCredential credential = brokerCredentialService.find(broker, TradingMode.PAPER)
                .orElseThrow(() -> new BrokerCredentialNotConfiguredException(broker, TradingMode.PAPER));

        String clientOrderId = UUID.randomUUID().toString();
        Order order = new Order(ticker, credential, broker, ticker.getAssetType(), side, quantity,
                request.takeProfitPrice(), request.stopLossPrice(), clientOrderId);
        order.setIndicatorSnapshot(computation.snapshot());
        order.setRequestedAmountUsd(request.amountUsd());
        order.setLeverage(request.leverage());
        order.setEntryOrderType(EntryOrderType.MARKET);
        order.setOrderMode(TradingMode.PAPER);
        Long orderId = orderRepository.save(order).getId();

        BrokerOrderRequest brokerRequest = new BrokerOrderRequest(clientOrderId, ticker.getSymbol(), ticker.getAssetType(),
                side, quantity, EntryOrderType.MARKET, null, request.takeProfitPrice(), request.stopLossPrice(),
                request.leverage());

        try {
            BrokerOrderResult result = adapter.placeOrder(brokerRequest, TradingMode.PAPER);
            return TradeOrderResponse.from(applyResult(orderId, result));
        } catch (BrokerAdapterAmbiguousOrderException e) {
            return TradeOrderResponse.from(applyOutcome(orderId, OrderStatus.SUBMISSION_UNKNOWN, e.getMessage(), null));
        } catch (BrokerAdapterUnavailableException e) {
            return TradeOrderResponse.from(applyOutcome(orderId, OrderStatus.FAILED,
                    "Broker unavailable after retries exhausted: " + e.getMessage() + ". Order was not submitted; safe to retry.",
                    null));
        } catch (BrokerAdapterRateLimitedException e) {
            String suffix = e.retryAfterSeconds() != null ? "; retry after " + e.retryAfterSeconds() + "s" : "";
            return TradeOrderResponse.from(applyOutcome(orderId, OrderStatus.FAILED,
                    "Rate limited by " + broker + suffix + ". Order was not submitted.", null));
        } catch (BrokerAdapterException e) {
            return TradeOrderResponse.from(applyOutcome(orderId, OrderStatus.FAILED, e.getMessage(), null));
        }
    }

    public List<OrderResponse> listOrders(int limit) {
        return orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * Re-polls the broker for one order's current status and persists whatever it reports — the only way an
     * ambiguous {@code SUBMISSION_UNKNOWN}/{@code PARTIALLY_PROTECTED} row (or a {@code PENDING} row orphaned by an
     * app crash mid-submission) ever gets resolved, since nothing polls automatically. If the broker has no record of
     * this {@code clientOrderId}, that's treated the same as {@code OrderService}'s own outage-reconciliation
     * wording elsewhere: confirmed not submitted, safe to retry. If the broker call itself throws, the stored row is
     * left completely untouched — overwriting a known status with a failed-read guess would be the actual bug here.
     */
    public OrderResponse refreshOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        Ticker ticker = order.getTicker();
        BrokerAdapter adapter = router.forAssetType(ticker.getAssetType());

        Optional<BrokerOrderResult> result;
        try {
            result = adapter.getOrderStatus(ticker.getSymbol(), order.getClientOrderId(), order.getOrderMode());
        } catch (BrokerAdapterException e) {
            throw new OrderRefreshUnavailableException(
                    "Could not refresh order status from " + order.getBroker() + ": " + e.getMessage());
        }

        Order updated = result.map(r -> applyResult(orderId, r))
                .orElseGet(() -> applyOutcome(orderId, OrderStatus.FAILED,
                        "Broker confirmed no record of this order — safe to retry.", null));
        return OrderResponse.from(updated);
    }

    private void validate(PlaceOrderRequest request, AssetType assetType, OrderSide side, BigDecimal price) {
        BigDecimal leverage = request.leverage();
        if (leverage.stripTrailingZeros().scale() > 0) {
            throw new InvalidTradeRequestException("Leverage must be a whole number.");
        }
        if (assetType == AssetType.STOCK && leverage.compareTo(BigDecimal.ONE) != 0) {
            throw new InvalidTradeRequestException("Stock orders cannot use leverage (must be 1x).");
        }
        if (assetType == AssetType.CRYPTO && (leverage.compareTo(BigDecimal.ONE) < 0
                || leverage.compareTo(BigDecimal.valueOf(MAX_CRYPTO_LEVERAGE)) > 0)) {
            throw new InvalidTradeRequestException("Leverage must be between 1x and " + MAX_CRYPTO_LEVERAGE + "x.");
        }

        BigDecimal takeProfit = request.takeProfitPrice();
        BigDecimal stopLoss = request.stopLossPrice();
        if (side == OrderSide.BUY) {
            if (takeProfit.compareTo(price) <= 0) {
                throw new InvalidTradeRequestException("Take-profit must be above the current price (" + price + ").");
            }
            if (stopLoss.compareTo(price) >= 0) {
                throw new InvalidTradeRequestException("Stop-loss must be below the current price (" + price + ").");
            }
        } else {
            if (takeProfit.compareTo(price) >= 0) {
                throw new InvalidTradeRequestException("Take-profit must be below the current price (" + price + ").");
            }
            if (stopLoss.compareTo(price) <= 0) {
                throw new InvalidTradeRequestException("Stop-loss must be above the current price (" + price + ").");
            }
        }
    }

    private Order applyResult(Long orderId, BrokerOrderResult result) {
        return applyOutcome(orderId, result.status(), result.rejectionReason(), result);
    }

    /** Not {@code @Transactional} at the {@link #submitOrder} level — each of these two short writes gets its own
     * transaction via {@code SimpleJpaRepository}, so the DB connection is never held open across the broker HTTP call. */
    private Order applyOutcome(Long orderId, OrderStatus status, String rejectionReason, BrokerOrderResult result) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order " + orderId + " vanished mid-submission"));
        order.setStatus(status);
        order.setRejectionReason(rejectionReason);
        order.setSubmittedAt(Instant.now());
        if (result != null) {
            order.setBrokerOrderId(result.brokerOrderId());
            if (result.filledPrice() != null) {
                order.setEntryPrice(result.filledPrice());
                order.setFilledAt(result.asOf());
            }
        }
        return orderRepository.save(order);
    }
}
