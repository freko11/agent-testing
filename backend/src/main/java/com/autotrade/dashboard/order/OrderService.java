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
import com.autotrade.dashboard.notification.NotificationService;
import com.autotrade.dashboard.risk.KillSwitchCancelSummary;
import com.autotrade.dashboard.risk.KillSwitchService;
import com.autotrade.dashboard.risk.RiskLimitService;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
import com.autotrade.dashboard.signal.SignalResponse;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.tradingmode.TradingModeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns a validated "click Trade" request into a real bracket order
 * (E5-F2-S1). Always recomputes the signal server-side rather than trusting
 * whatever {@link SignalResponse} the frontend has cached — a stale call or
 * price is a real risk for a leveraged bracket order. {@code TradingMode} is
 * read from the global switch (E6-F1-S1, {@link TradingModeService#current()})
 * rather than taken from the request — a single app-wide mode, not a
 * per-order choice. In practice this is still always {@code PAPER}: {@code
 * TradingModeService.switchTo} unconditionally rejects {@code LIVE} until
 * E6-F1-S2/S3's threshold and consent gates exist, and neither credential
 * bootstrap (E4-F2-S1/E4-F3-S1) seeds a {@code LIVE} credential yet either.
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
 * ticker/request, no actionable signal, no credential configured, an engaged
 * {@link KillSwitchService kill switch} (E6-F2-S2), or a breached {@link
 * RiskLimitService risk cap} — per-order (E6-F2-S1) or portfolio-level
 * aggregate exposure (E6-F2-S3)) skip creating an {@code Order} row at all.
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

    /** Statuses {@link #cancelAllOpenOrders} treats as already-resolved and skips. Deliberately narrow — {@code
     * PARTIALLY_PROTECTED}/{@code SUBMISSION_UNKNOWN} rows are still attempted, relying on each adapter's own
     * idempotent no-op on an already-terminal order rather than this list re-deriving "cancellable" per status. */
    private static final List<OrderStatus> TERMINAL_STATUSES =
            List.of(OrderStatus.FILLED, OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.FAILED);

    private final SignalService signalService;
    private final BrokerAdapterRouter router;
    private final BrokerCredentialService brokerCredentialService;
    private final OrderRepository orderRepository;
    private final SignalCallEntryRepository signalCallEntryRepository;
    private final OrderAuditEntryRepository orderAuditEntryRepository;
    private final NotificationService notificationService;
    private final TradingModeService tradingModeService;
    private final RiskLimitService riskLimitService;
    private final KillSwitchService killSwitchService;

    public OrderService(SignalService signalService, BrokerAdapterRouter router,
                         BrokerCredentialService brokerCredentialService, OrderRepository orderRepository,
                         SignalCallEntryRepository signalCallEntryRepository,
                         OrderAuditEntryRepository orderAuditEntryRepository, NotificationService notificationService,
                         TradingModeService tradingModeService, RiskLimitService riskLimitService,
                         KillSwitchService killSwitchService) {
        this.signalService = signalService;
        this.router = router;
        this.brokerCredentialService = brokerCredentialService;
        this.orderRepository = orderRepository;
        this.signalCallEntryRepository = signalCallEntryRepository;
        this.orderAuditEntryRepository = orderAuditEntryRepository;
        this.notificationService = notificationService;
        this.tradingModeService = tradingModeService;
        this.riskLimitService = riskLimitService;
        this.killSwitchService = killSwitchService;
    }

    public TradeOrderResponse submitOrder(String symbol, PlaceOrderRequest request) {
        killSwitchService.assertNotEngaged();
        SignalService.SignalComputation computation = signalService.computeSignalWithProvenance(symbol, SIGNAL_LIMIT);
        SignalResponse signal = computation.response();

        if (signal.call() == SignalCall.HOLD) {
            throw new SignalNotActionableException(symbol, signal.matchedRule());
        }

        Ticker ticker = computation.snapshot().getTicker();
        BigDecimal price = signal.indicators().price();
        OrderSide side = signal.call() == SignalCall.BUY ? OrderSide.BUY : OrderSide.SELL;

        validate(request, ticker.getAssetType(), side, price);
        riskLimitService.enforcePerOrderCaps(ticker.getAssetType(), request.amountUsd(), request.leverage());

        TradingMode mode = tradingModeService.current();
        BigDecimal openNotionalUsd = orderRepository.sumOpenNotionalUsd(mode, TERMINAL_STATUSES);
        BigDecimal newOrderNotionalUsd = request.amountUsd().multiply(request.leverage());
        riskLimitService.enforceAggregateExposureCap(openNotionalUsd, newOrderNotionalUsd);

        BigDecimal quantity = request.amountUsd().divide(price, 8, RoundingMode.DOWN);
        if (quantity.signum() <= 0) {
            throw new InvalidTradeRequestException(
                    "Requested amount is too small to produce a positive quantity at the current price (" + price + ").");
        }

        BrokerAdapter adapter = router.forAssetType(ticker.getAssetType());
        Broker broker = adapter.broker();
        BrokerCredential credential = brokerCredentialService.find(broker, mode)
                .orElseThrow(() -> new BrokerCredentialNotConfiguredException(broker, mode));

        SignalCallEntry signalCallEntry = signalCallEntryRepository
                .findTopByIndicatorSnapshot_IdOrderByIdDesc(computation.snapshot().getId())
                .orElseThrow(() -> new IllegalStateException("SignalCallEntry vanished right after being persisted"));

        String clientOrderId = UUID.randomUUID().toString();
        Order order = new Order(ticker, credential, broker, ticker.getAssetType(), side, quantity,
                request.takeProfitPrice(), request.stopLossPrice(), clientOrderId);
        order.setIndicatorSnapshot(computation.snapshot());
        order.setRequestedAmountUsd(request.amountUsd());
        order.setLeverage(request.leverage());
        order.setEntryOrderType(EntryOrderType.MARKET);
        order.setOrderMode(mode);
        Long orderId = orderRepository.save(order).getId();

        BrokerOrderRequest brokerRequest = new BrokerOrderRequest(clientOrderId, ticker.getSymbol(), ticker.getAssetType(),
                side, quantity, EntryOrderType.MARKET, null, request.takeProfitPrice(), request.stopLossPrice(),
                request.leverage());

        try {
            BrokerOrderResult result = adapter.placeOrder(brokerRequest, mode);
            return TradeOrderResponse.from(recordAuditEntry(applyResult(orderId, result), signalCallEntry));
        } catch (BrokerAdapterAmbiguousOrderException e) {
            return TradeOrderResponse.from(recordAuditEntry(
                    applyOutcome(orderId, OrderStatus.SUBMISSION_UNKNOWN, e.getMessage(), null), signalCallEntry));
        } catch (BrokerAdapterUnavailableException e) {
            return TradeOrderResponse.from(recordAuditEntry(applyOutcome(orderId, OrderStatus.FAILED,
                    "Broker unavailable after retries exhausted: " + e.getMessage() + ". Order was not submitted; safe to retry.",
                    null), signalCallEntry));
        } catch (BrokerAdapterRateLimitedException e) {
            String suffix = e.retryAfterSeconds() != null ? "; retry after " + e.retryAfterSeconds() + "s" : "";
            return TradeOrderResponse.from(recordAuditEntry(applyOutcome(orderId, OrderStatus.FAILED,
                    "Rate limited by " + broker + suffix + ". Order was not submitted.", null), signalCallEntry));
        } catch (BrokerAdapterException e) {
            return TradeOrderResponse.from(
                    recordAuditEntry(applyOutcome(orderId, OrderStatus.FAILED, e.getMessage(), null), signalCallEntry));
        }
    }

    /**
     * Best-effort cancels every non-terminal {@code Order} this app has submitted, across both brokers
     * (E6-F2-S2's kill switch). One order's broker-adapter failure never stops the sweep for the rest — each
     * is caught independently so, say, a Binance outage doesn't leave Alpaca orders uncancelled. A failed
     * cancel leaves that {@code Order} row untouched (same "don't overwrite a known status with a failed-read
     * guess" rule {@link #refreshOrder} already follows) rather than guessing at its new state.
     */
    public KillSwitchCancelSummary cancelAllOpenOrders() {
        List<Order> candidates = orderRepository.findByStatusNotIn(TERMINAL_STATUSES);
        int cancelled = 0;
        List<String> failures = new ArrayList<>();
        for (Order order : candidates) {
            try {
                BrokerAdapter adapter = router.forAssetType(order.getAssetType());
                BrokerOrderResult result = adapter.cancelOrder(order.getTicker().getSymbol(), order.getClientOrderId(),
                        order.getOrderMode());
                applyResult(order.getId(), result);
                cancelled++;
            } catch (BrokerAdapterException e) {
                failures.add(order.getClientOrderId() + ": " + e.getMessage());
            }
        }
        return new KillSwitchCancelSummary(candidates.size(), cancelled, failures.size(), failures);
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

    /**
     * Renders order history for a date range as CSV (E5-F3-S2). {@code start}/{@code end} are treated as UTC
     * calendar days (inclusive both ends) — this codebase has no established "user local time" concept anywhere
     * else (every timestamp is UTC {@code Instant}; the one existing timezone, {@code MarketHoursService}'s
     * {@code America/New_York}, is NYSE-specific, not user-local), so introducing one here would be out of
     * proportion for this story. {@code mode} is optional and, when omitted, exports every mode — matching {@link
     * #listOrders}'s own no-filter default — since only {@code PAPER} is reachable until E6 seeds {@code LIVE}
     * credentials.
     */
    public String exportOrdersCsv(LocalDate start, LocalDate end, TradingMode mode) {
        if (start.isAfter(end)) {
            throw new InvalidTradeRequestException("start date must not be after end date.");
        }
        Instant startInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endInstant = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        List<Order> orders = mode != null
                ? orderRepository.findByOrderModeAndCreatedAtBetweenOrderByCreatedAtAsc(mode, startInstant, endInstant)
                : orderRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(startInstant, endInstant);

        List<Long> snapshotIds = orders.stream()
                .filter(order -> order.getIndicatorSnapshot() != null)
                .map(order -> order.getIndicatorSnapshot().getId())
                .distinct()
                .toList();
        Map<Long, SignalCallEntry> signalCallsBySnapshotId = snapshotIds.isEmpty()
                ? Map.of()
                : signalCallEntryRepository.findByIndicatorSnapshot_IdIn(snapshotIds).stream()
                        .collect(Collectors.toMap(entry -> entry.getIndicatorSnapshot().getId(), Function.identity(),
                                (first, second) -> second.getId() > first.getId() ? second : first));

        return OrderCsvExporter.export(orders, signalCallsBySnapshotId);
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

    /** Writes the one, never-updated {@code OrderAuditEntry} row for an order (E6-F3-S1), capturing its first
     * resolved outcome from {@link #submitOrder} only — deliberately not called from {@link #refreshOrder} or
     * {@link #cancelAllOpenOrders}, so a later status change to {@code order} (e.g. {@code SUBMISSION_UNKNOWN}
     * resolving to {@code FILLED}, or a cancellation) is never reflected here. This freezes the decision made at
     * order-placement time; {@code Order}/{@code OrderResponse}/CSV export remain the source of truth for an
     * order's current/live status. */
    private Order recordAuditEntry(Order order, SignalCallEntry signalCallEntry) {
        orderAuditEntryRepository.save(new OrderAuditEntry(order, signalCallEntry,
                signalCallEntry.getRuleTableVersion(), order.getStatus(),
                order.getRejectionReason(), order.getBrokerOrderId(), order.getEntryPrice()));
        return order;
    }

    /** Not {@code @Transactional} at the {@link #submitOrder} level — each of these two short writes gets its own
     * transaction via {@code SimpleJpaRepository}, so the DB connection is never held open across the broker HTTP call.
     * Notifies (E5-F4-S1) only on a genuine status transition — this is what stops a manual {@link #refreshOrder}
     * re-poll of an already-terminal order (e.g. re-fetching an already-{@code FILLED} order) from re-notifying.
     * Also called by {@link #refreshOrder}/{@link #cancelAllOpenOrders} to keep updating {@code Order} after
     * submission — unlike {@link #recordAuditEntry}, which only ever fires once, from {@link #submitOrder}. */
    private Order applyOutcome(Long orderId, OrderStatus status, String rejectionReason, BrokerOrderResult result) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order " + orderId + " vanished mid-submission"));
        OrderStatus previousStatus = order.getStatus();
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
        Order saved = orderRepository.save(order);
        if (previousStatus != status) {
            notificationService.recordOrderOutcome(saved, previousStatus);
        }
        return saved;
    }
}
