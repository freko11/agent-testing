package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Real {@link BrokerAdapter} for Binance's USD&#9328;-M Futures Testnet trading
 * API — {@code https://testnet.binancefuture.com} for {@code PAPER}, {@code
 * https://fapi.binance.com} for {@code LIVE}. {@code getAccountStatus}/{@code
 * getPosition} were built in E4-F3-S1; {@code placeOrder}/{@code
 * getOrderStatus}/{@code cancelOrder} are E4-F3-S2, confirmed with the user
 * before implementation (money-safety weight):
 *
 * <p><b>Bracket construction.</b> Binance Futures has no single-call bracket
 * order the way Alpaca does, so a "bracket" here is three separate orders:
 * {@code POST /fapi/v1/leverage} then a MARKET entry, then a {@code
 * STOP_MARKET} stop-loss and a {@code TAKE_PROFIT_MARKET} take-profit (both
 * {@code closePosition=true}, so they close the whole position regardless of
 * fills — no separate quantity-tracking needed). All three orders' {@code
 * newClientOrderId}s are derived deterministically from the app's {@code
 * clientOrderId} via SHA-256 (see {@link #legIds}) rather than suffixed
 * directly onto it, since Binance's id charset/length limit (36 chars) is
 * incompatible with this app's own {@code orders.client_order_id VARCHAR2(64)}
 * column. Determinism is what makes every leg idempotent on retry: {@code
 * placeOrder} always checks for an existing leg by its derived id before
 * ever attempting to create it (a "check first" GET, not "POST then catch a
 * duplicate-id error" the way {@code AlpacaTradingAdapter} does), so a full
 * replay of {@code placeOrder} — whether the caller retries with the same
 * {@code clientOrderId} or {@link RetryingBrokerAdapter} retries the whole
 * call — safely re-discovers whatever already exists instead of risking a
 * duplicate.
 *
 * <p><b>MARKET entries only for v1</b> (confirmed, a real narrowing vs.
 * Alpaca's LIMIT-or-MARKET support) — a resting LIMIT entry might not fill
 * before this call returns, and the {@code closePosition=true} exit legs
 * need an already-open position; supporting LIMIT would need a
 * poll-until-filled mechanism this story doesn't build. A non-MARKET
 * request is rejected fatally, pre-HTTP, same posture as {@link
 * #MAX_LEVERAGE} below.
 *
 * <p><b>Partial-failure handling — the real crux of this story</b> (confirmed):
 * once the entry order is confirmed {@code FILLED}/{@code PARTIALLY_FILLED},
 * {@code placeOrder} never throws again — every subsequent failure (placing
 * either exit leg) is caught, bounded-retried locally twice, and if still
 * failing, reported as a value rather than an exception. The stop-loss leg
 * is attempted before the take-profit leg (confirmed: the protective leg
 * gets priority when only one fits). If either leg is ultimately missing,
 * the result carries {@link OrderStatus#PARTIALLY_PROTECTED} — the entry's
 * real order id/fill price, plus a {@code rejectionReason} naming which
 * leg(s) are missing — rather than a bare success that would hide a real
 * unprotected leveraged position. Auto-flattening the position on leg
 * failure was deliberately rejected (confirmed): closing is itself another
 * order action with the same failure modes, and could realize a worse loss
 * than surfacing the state.
 *
 * <p><b>Leverage bound</b> (confirmed): {@link #MAX_LEVERAGE} = 20, a plain
 * hardcoded adapter-intrinsic ceiling — not Binance's real per-symbol limit
 * (1x-125x via {@code /fapi/v1/leverageBracket}, an extra live-data
 * dependency this codebase avoids for this kind of check, same bias as
 * {@code MarketHoursService}'s hardcoded calendar) and explicitly distinct
 * from E6-F2-S1's later <em>user-configurable</em> risk cap. Binance's own
 * per-symbol authoritative check still happens at {@code POST
 * /fapi/v1/leverage} time; a rejection there maps to a normal {@code
 * REJECTED} result, not an exception.
 *
 * <p>{@code getOrderStatus} reports a composite status across all three
 * legs: if the entry hasn't filled yet, that status is reported as-is; once
 * filled, a missing/terminated-without-filling exit leg downgrades the
 * report to {@code PARTIALLY_PROTECTED}, and an exit leg that itself shows
 * {@code FILLED} (it triggered, closing the position) reports as {@code
 * CANCELLED} — the closest existing vocabulary for "no longer live, no
 * rejection" (which leg fired and at what price isn't preserved by this
 * interface today, a flagged, accepted gap). {@code cancelOrder} cancels
 * only the entry leg (same "cancel the order, not the position" scope
 * Alpaca already has for its own bracket legs) — idempotent no-op on any
 * terminal composite status, including {@code PARTIALLY_PROTECTED}.
 */
public class BinanceFuturesTradingAdapter implements BrokerAdapter {

    private static final Logger log = LoggerFactory.getLogger(BinanceFuturesTradingAdapter.class);

    private static final String API_KEY_HEADER = "X-MBX-APIKEY";
    private static final long RECV_WINDOW_MILLIS = 5_000;
    private static final long ORDER_DOES_NOT_EXIST_CODE = -2013L;
    private static final int MAX_LEVERAGE = 20;
    private static final int EXIT_LEG_MAX_ATTEMPTS = 2;
    private static final long EXIT_LEG_RETRY_PAUSE_MILLIS = 200;
    // Hyphen allowed only for this codebase's own "-NOACTIVITY" test-symbol convention
    // (see BrokerAdapterContractTest); real Binance symbols are plain alphanumeric.
    private static final Pattern SYMBOL_PATTERN = Pattern.compile("^[A-Z0-9-]{1,20}$");

    private final Map<TradingMode, RestClient> restClientsByMode;
    private final BrokerCredentialService credentialService;
    private final Clock clock;
    private final ObjectMapper errorBodyMapper = new ObjectMapper();

    public BinanceFuturesTradingAdapter(Map<TradingMode, RestClient> restClientsByMode,
                                         BrokerCredentialService credentialService,
                                         Clock clock) {
        this.restClientsByMode = restClientsByMode;
        this.credentialService = credentialService;
        this.clock = clock;
    }

    @Override
    public AssetType supportedAssetType() {
        return AssetType.CRYPTO;
    }

    @Override
    public Broker broker() {
        return Broker.BINANCE;
    }

    @Override
    public BrokerOrderResult placeOrder(BrokerOrderRequest request, TradingMode mode) {
        validateOrderRequest(request);
        validateSymbol(request.symbol());
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);
        LegIds legIds = legIds(request.clientOrderId());

        BinanceOrderResponse entry = findOrder(restClient, creds, request.symbol(), legIds.entry());
        if (entry == null) {
            try {
                setLeverage(restClient, creds, request.symbol(), request.leverage().intValueExact());
            } catch (RuntimeException e) {
                return rejectedResultOrRethrow(e, request.clientOrderId(),
                        "Binance rejected leverage=" + request.leverage() + " for " + request.symbol());
            }
            try {
                entry = placeEntryOrder(restClient, creds, request, legIds.entry());
            } catch (RuntimeException e) {
                return rejectedResultOrRethrow(e, request.clientOrderId(), "Binance rejected the entry order");
            }
            if (entry == null) {
                throw new BrokerAdapterException(Broker.BINANCE, "Binance returned an empty order response");
            }
        }

        OrderStatus entryStatus = mapStatus(entry.status());
        String brokerOrderId = String.valueOf(entry.orderId());
        if (entryStatus != OrderStatus.FILLED && entryStatus != OrderStatus.PARTIALLY_FILLED) {
            return new BrokerOrderResult(request.clientOrderId(), brokerOrderId, entryStatus, null, null, clock.instant());
        }

        OrderSide closeSide = request.side() == OrderSide.BUY ? OrderSide.SELL : OrderSide.BUY;
        boolean stopLossPlaced = ensureExitLeg(restClient, creds, request.symbol(), legIds.stopLoss(),
                "STOP_MARKET", request.stopLossPrice(), closeSide);
        boolean takeProfitPlaced = ensureExitLeg(restClient, creds, request.symbol(), legIds.takeProfit(),
                "TAKE_PROFIT_MARKET", request.takeProfitPrice(), closeSide);

        if (stopLossPlaced && takeProfitPlaced) {
            return new BrokerOrderResult(request.clientOrderId(), brokerOrderId, entryStatus, entry.avgPrice(), null, clock.instant());
        }
        String missingLegs = missingLegsDescription(stopLossPlaced, takeProfitPlaced);
        return new BrokerOrderResult(request.clientOrderId(), brokerOrderId, OrderStatus.PARTIALLY_PROTECTED, entry.avgPrice(),
                "Entry filled (orderId=" + entry.orderId() + ") but " + missingLegs
                        + " leg(s) could not be placed after retry. Position is open and may be unprotected.",
                clock.instant());
    }

    @Override
    public Optional<BrokerOrderResult> getOrderStatus(String symbol, String clientOrderId, TradingMode mode) {
        validateSymbol(symbol);
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);
        LegIds legIds = legIds(clientOrderId);

        BinanceOrderResponse entry = findOrder(restClient, creds, symbol, legIds.entry());
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(compositeResult(restClient, creds, symbol, clientOrderId, legIds, entry));
    }

    private BrokerOrderResult compositeResult(RestClient restClient, Credentials creds, String symbol, String clientOrderId,
                                                LegIds legIds, BinanceOrderResponse entry) {
        OrderStatus entryStatus = mapStatus(entry.status());
        String brokerOrderId = String.valueOf(entry.orderId());
        if (entryStatus != OrderStatus.FILLED && entryStatus != OrderStatus.PARTIALLY_FILLED) {
            return new BrokerOrderResult(clientOrderId, brokerOrderId, entryStatus, null, null, clock.instant());
        }

        BinanceOrderResponse stopLoss = findOrder(restClient, creds, symbol, legIds.stopLoss());
        BinanceOrderResponse takeProfit = findOrder(restClient, creds, symbol, legIds.takeProfit());

        if (isTriggered(stopLoss) || isTriggered(takeProfit)) {
            // A leg that itself FILLED closed the position — no longer live, no rejection either.
            return new BrokerOrderResult(clientOrderId, brokerOrderId, OrderStatus.CANCELLED, entry.avgPrice(), null, clock.instant());
        }
        if (isMissingProtection(stopLoss) || isMissingProtection(takeProfit)) {
            return new BrokerOrderResult(clientOrderId, brokerOrderId, OrderStatus.PARTIALLY_PROTECTED, entry.avgPrice(),
                    "Entry filled (orderId=" + entry.orderId() + ") but a take-profit/stop-loss leg is missing.",
                    clock.instant());
        }
        return new BrokerOrderResult(clientOrderId, brokerOrderId, entryStatus, entry.avgPrice(), null, clock.instant());
    }

    @Override
    public BrokerOrderResult cancelOrder(String symbol, String clientOrderId, TradingMode mode) {
        Optional<BrokerOrderResult> existing = getOrderStatus(symbol, clientOrderId, mode);
        if (existing.isEmpty()) {
            return new BrokerOrderResult(clientOrderId, null, OrderStatus.FAILED, null, "Unknown clientOrderId", clock.instant());
        }
        BrokerOrderResult current = existing.get();
        if (isTerminal(current.status())) {
            // Already FILLED/CANCELLED/REJECTED/FAILED/PARTIALLY_PROTECTED — idempotent no-op, same convention as MockBrokerAdapter/AlpacaTradingAdapter.
            return current;
        }

        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);
        deleteOrder(restClient, creds, symbol, legIds(clientOrderId).entry());

        // Cancels only the entry leg — same "cancel the order, not the position" scope Alpaca already has for its own bracket legs.
        return getOrderStatus(symbol, clientOrderId, mode).orElse(current);
    }

    @Override
    public Optional<BrokerPosition> getPosition(String symbol, TradingMode mode) {
        validateSymbol(symbol);
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        BinancePositionResponse[] response =
                signedGet(restClient, creds, "/fapi/v3/positionRisk", params, BinancePositionResponse[].class);

        if (response == null || response.length == 0) {
            return Optional.empty();
        }
        BinancePositionResponse position = response[0];
        if (position.positionAmt() == null || position.positionAmt().signum() == 0) {
            // Binance reports a flat position as a row with positionAmt "0", not an absent row.
            return Optional.empty();
        }
        return Optional.of(new BrokerPosition(
                symbol, AssetType.CRYPTO, position.positionAmt(), position.entryPrice(),
                position.unrealizedProfit(), clock.instant()));
    }

    @Override
    public BrokerAccountStatus getAccountStatus(TradingMode mode) {
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);

        BinanceAccountResponse response =
                signedGet(restClient, creds, "/fapi/v3/account", new LinkedHashMap<>(), BinanceAccountResponse.class);
        if (response == null) {
            throw new BrokerAdapterException(Broker.BINANCE, "Binance returned an empty account response");
        }

        List<BrokerAccountStatus.AssetBalance> balances = response.assets() == null ? List.of()
                : response.assets().stream()
                        .map(a -> new BrokerAccountStatus.AssetBalance(
                                a.asset(), a.availableBalance(), a.walletBalance().subtract(a.availableBalance())))
                        .toList();
        return new BrokerAccountStatus(
                Broker.BINANCE, mode, balances, response.totalMarginBalance(), response.availableBalance(), clock.instant());
    }

    private void validateOrderRequest(BrokerOrderRequest request) {
        if (request.assetType() != AssetType.CRYPTO) {
            throw new BrokerAdapterException(Broker.BINANCE,
                    "BinanceFuturesTradingAdapter only supports CRYPTO orders, got " + request.assetType());
        }
        if (request.entryOrderType() != EntryOrderType.MARKET) {
            throw new BrokerAdapterException(Broker.BINANCE,
                    "BinanceFuturesTradingAdapter only supports MARKET entries for v1 (E4-F3-S2 scope), got "
                            + request.entryOrderType());
        }
        BigDecimal leverage = request.leverage();
        if (leverage == null || leverage.stripTrailingZeros().scale() > 0
                || leverage.compareTo(BigDecimal.ONE) < 0 || leverage.compareTo(BigDecimal.valueOf(MAX_LEVERAGE)) > 0) {
            throw new BrokerAdapterException(Broker.BINANCE,
                    "Leverage must be a whole number between 1 and " + MAX_LEVERAGE + ", got " + leverage);
        }
    }

    /**
     * Every Binance call here signs a literal, non-re-encoded query string
     * (see class Javadoc) — {@code symbol} is the one caller-supplied value
     * built directly into it. {@code TickerController}'s own validation is
     * only {@code @NotBlank @Size(max = 20)}, with no character-class
     * restriction, so an unvalidated symbol could inject extra query
     * parameters into a real money-moving Binance request. Real Binance
     * Futures symbols are plain uppercase alphanumeric (e.g. {@code
     * BTCUSDT}); this check is fatal, pre-HTTP, same posture as {@link
     * #validateOrderRequest}.
     */
    private void validateSymbol(String symbol) {
        if (symbol == null || !SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new BrokerAdapterException(Broker.BINANCE, "Invalid symbol: " + symbol);
        }
    }

    private void setLeverage(RestClient restClient, Credentials creds, String symbol, int leverage) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("leverage", String.valueOf(leverage));
        signedRequest(restClient, creds, HttpMethod.POST, "/fapi/v1/leverage", params, Object.class, null);
    }

    private BinanceOrderResponse placeEntryOrder(RestClient restClient, Credentials creds, BrokerOrderRequest request, String entryLegId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", request.symbol());
        params.put("side", request.side().name());
        params.put("type", "MARKET");
        params.put("quantity", request.quantity().toPlainString());
        params.put("newClientOrderId", entryLegId);
        params.put("newOrderRespType", "RESULT");
        return signedRequest(restClient, creds, HttpMethod.POST, "/fapi/v1/order", params, BinanceOrderResponse.class, null);
    }

    /**
     * Places one exit leg (stop-loss or take-profit) if it doesn't already
     * exist, bounded-retrying up to {@link #EXIT_LEG_MAX_ATTEMPTS} times.
     * Every failure (transient, rate-limited, or a fatal business rejection)
     * is caught uniformly here and never rethrown — the caller only learns
     * whether the leg ended up placed, per this story's confirmed
     * partial-failure design.
     */
    private boolean ensureExitLeg(RestClient restClient, Credentials creds, String symbol, String legClientOrderId,
                                   String orderType, BigDecimal stopPrice, OrderSide closeSide) {
        for (int attempt = 1; attempt <= EXIT_LEG_MAX_ATTEMPTS; attempt++) {
            try {
                if (findOrder(restClient, creds, symbol, legClientOrderId) != null) {
                    return true; // already placed on a prior attempt/replay
                }
                Map<String, String> params = new LinkedHashMap<>();
                params.put("symbol", symbol);
                params.put("side", closeSide.name());
                params.put("type", orderType);
                params.put("stopPrice", stopPrice.toPlainString());
                params.put("closePosition", "true");
                params.put("newClientOrderId", legClientOrderId);
                params.put("newOrderRespType", "RESULT");
                signedRequest(restClient, creds, HttpMethod.POST, "/fapi/v1/order", params, BinanceOrderResponse.class, null);
                return true;
            } catch (RuntimeException e) {
                if (attempt >= EXIT_LEG_MAX_ATTEMPTS || !pauseBeforeRetry()) {
                    log.warn("broker=BINANCE symbol={} legClientOrderId={} orderType={} attempt={}/{} - exit leg "
                                    + "could not be placed, giving up; position may be left unprotected",
                            symbol, legClientOrderId, orderType, attempt, EXIT_LEG_MAX_ATTEMPTS, e);
                    return false;
                }
                log.warn("broker=BINANCE symbol={} legClientOrderId={} orderType={} attempt={}/{} - exit leg "
                                + "placement failed, retrying",
                        symbol, legClientOrderId, orderType, attempt, EXIT_LEG_MAX_ATTEMPTS, e);
            }
        }
        return false;
    }

    private boolean pauseBeforeRetry() {
        try {
            Thread.sleep(EXIT_LEG_RETRY_PAUSE_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String missingLegsDescription(boolean stopLossPlaced, boolean takeProfitPlaced) {
        if (!stopLossPlaced && !takeProfitPlaced) {
            return "STOP_LOSS and TAKE_PROFIT";
        }
        return !stopLossPlaced ? "STOP_LOSS" : "TAKE_PROFIT";
    }

    private void deleteOrder(RestClient restClient, Credentials creds, String symbol, String origClientOrderId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("origClientOrderId", origClientOrderId);
        try {
            signedRequest(restClient, creds, HttpMethod.DELETE, "/fapi/v1/order", params, BinanceOrderResponse.class, null);
        } catch (BrokerAdapterRateLimitedException | BrokerAdapterTransientException e) {
            throw e;
        } catch (BrokerAdapterException e) {
            // Order became not-cancelable between our status check and this call (e.g. just filled) — idempotent no-op, same convention as AlpacaTradingAdapter.
            log.debug("broker=BINANCE symbol={} origClientOrderId={} - cancel rejected, treating as "
                    + "already-terminal (idempotent no-op)", symbol, origClientOrderId, e);
        }
    }

    private BinanceOrderResponse findOrder(RestClient restClient, Credentials creds, String symbol, String origClientOrderId) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("origClientOrderId", origClientOrderId);
        return signedRequest(restClient, creds, HttpMethod.GET, "/fapi/v1/order", params, BinanceOrderResponse.class, ORDER_DOES_NOT_EXIST_CODE);
    }

    private BrokerOrderResult rejectedResultOrRethrow(RuntimeException e, String clientOrderId, String context) {
        if (e instanceof BrokerAdapterRateLimitedException || e instanceof BrokerAdapterTransientException) {
            throw e;
        }
        if (e instanceof BrokerAdapterException bae) {
            return rejectedResult(clientOrderId, context + ": " + bae.getMessage());
        }
        throw e;
    }

    private BrokerOrderResult rejectedResult(String clientOrderId, String reason) {
        return new BrokerOrderResult(clientOrderId, null, OrderStatus.REJECTED, null, reason, clock.instant());
    }

    private static boolean isTriggered(BinanceOrderResponse leg) {
        return leg != null && "FILLED".equals(leg.status());
    }

    private static boolean isMissingProtection(BinanceOrderResponse leg) {
        if (leg == null) {
            return true;
        }
        return "CANCELED".equals(leg.status()) || "EXPIRED".equals(leg.status()) || "REJECTED".equals(leg.status());
    }

    private static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.FILLED || status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED
                || status == OrderStatus.FAILED || status == OrderStatus.PARTIALLY_PROTECTED;
    }

    private static OrderStatus mapStatus(String binanceStatus) {
        if (binanceStatus == null) {
            return OrderStatus.SUBMITTED;
        }
        return switch (binanceStatus) {
            case "FILLED" -> OrderStatus.FILLED;
            case "PARTIALLY_FILLED" -> OrderStatus.PARTIALLY_FILLED;
            case "CANCELED", "EXPIRED" -> OrderStatus.CANCELLED;
            case "REJECTED" -> OrderStatus.REJECTED;
            // NEW, PENDING_CANCEL — still resting/in flight.
            default -> OrderStatus.SUBMITTED;
        };
    }

    /**
     * Derives the three broker-side {@code newClientOrderId}s for a
     * bracket's entry/stop-loss/take-profit legs from the app's single
     * {@code clientOrderId}, via SHA-256 rather than direct suffixing —
     * Binance's id charset/length limit (36 chars) is incompatible with
     * this app's own {@code orders.client_order_id VARCHAR2(64)} column.
     * Deterministic: the same app {@code clientOrderId} always derives the
     * same three leg ids, which is what makes every leg idempotent on retry.
     */
    private static LegIds legIds(String appClientOrderId) {
        String prefix = HexFormat.of().formatHex(sha256(appClientOrderId)).substring(0, 30);
        return new LegIds(prefix + "-E", prefix + "-T", prefix + "-S");
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private <T> T signedGet(RestClient restClient, Credentials creds, String path, Map<String, String> params, Class<T> responseType) {
        return signedRequest(restClient, creds, HttpMethod.GET, path, params, responseType, null);
    }

    /**
     * The single HTTP+signing primitive every Binance call goes through.
     * {@code ignorableErrorCode}, when non-null, treats that specific
     * Binance error code as "not found" (returns {@code null}) instead of
     * throwing — used only by {@link #findOrder} for {@code -2013} ("Order
     * does not exist"), the mechanism that makes {@code placeOrder}'s
     * check-first idempotency possible.
     */
    private <T> T signedRequest(RestClient restClient, Credentials creds, HttpMethod method, String path,
                                  Map<String, String> params, Class<T> responseType, Long ignorableErrorCode) {
        Map<String, String> signedParams = new LinkedHashMap<>(params);
        signedParams.put("timestamp", String.valueOf(clock.millis()));
        signedParams.put("recvWindow", String.valueOf(RECV_WINDOW_MILLIS));
        String signedQuery = signedQuery(signedParams, creds.apiSecret());

        try {
            return restClient.method(method)
                    .uri(path + "?" + signedQuery)
                    .header(API_KEY_HEADER, creds.apiKey())
                    .retrieve()
                    .body(responseType);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BrokerAdapterRateLimitedException(Broker.BINANCE, retryAfterSeconds(e));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 418) {
                // IP auto-banned after repeated 429s — treat the same as a rate limit for retry purposes.
                throw new BrokerAdapterRateLimitedException(Broker.BINANCE, retryAfterSeconds(e));
            }
            BinanceErrorResponse error = parseError(e);
            if (ignorableErrorCode != null && error != null && ignorableErrorCode.equals(error.code())) {
                return null;
            }
            throw new BrokerAdapterException(Broker.BINANCE, "Binance rejected the request: " + errorMessage(e), e);
        } catch (RestClientException e) {
            throw transientOrFatal(e);
        }
    }

    private String signedQuery(Map<String, String> params, String apiSecret) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!query.isEmpty()) {
                query.append('&');
            }
            query.append(entry.getKey()).append('=').append(entry.getValue());
        }
        String signature = hmacSha256Hex(apiSecret, query.toString());
        return query.append("&signature=").append(signature).toString();
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BrokerAdapterException(Broker.BINANCE, "Failed to sign Binance request", e);
        }
    }

    private Credentials resolveCredential(TradingMode mode) {
        BrokerCredential stored = credentialService.find(Broker.BINANCE, mode)
                .orElseThrow(() -> new BrokerAdapterException(Broker.BINANCE,
                        "No Binance trading credential configured for mode " + mode));
        BrokerCredentialService.DecryptedCredential decrypted = credentialService.readDecrypted(stored);
        return new Credentials(decrypted.apiKey(), decrypted.apiSecret());
    }

    private RestClient restClientFor(TradingMode mode) {
        RestClient restClient = restClientsByMode.get(mode);
        if (restClient == null) {
            throw new BrokerAdapterException(Broker.BINANCE, "No Binance trading RestClient configured for mode " + mode);
        }
        return restClient;
    }

    private RuntimeException transientOrFatal(RestClientException e) {
        if (e instanceof HttpServerErrorException || e instanceof ResourceAccessException) {
            return new BrokerAdapterTransientException(Broker.BINANCE, "Binance request failed: " + e.getMessage(), e);
        }
        return new BrokerAdapterException(Broker.BINANCE, "Binance request failed: " + e.getMessage(), e);
    }

    private static Long retryAfterSeconds(HttpStatusCodeException e) {
        String header = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
        if (header == null) {
            return null;
        }
        try {
            return Long.parseLong(header);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    private String errorMessage(HttpStatusCodeException e) {
        BinanceErrorResponse error = parseError(e);
        return error != null && error.msg() != null ? error.msg() : e.getMessage();
    }

    private BinanceErrorResponse parseError(HttpStatusCodeException e) {
        try {
            return errorBodyMapper.readValue(e.getResponseBodyAsString(), BinanceErrorResponse.class);
        } catch (Exception parseFailure) {
            return null;
        }
    }

    private record Credentials(String apiKey, String apiSecret) {
    }

    private record LegIds(String entry, String takeProfit, String stopLoss) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BinanceAccountAsset(
            @JsonProperty("asset") String asset,
            @JsonProperty("walletBalance") BigDecimal walletBalance,
            @JsonProperty("availableBalance") BigDecimal availableBalance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BinanceAccountResponse(
            @JsonProperty("totalMarginBalance") BigDecimal totalMarginBalance,
            @JsonProperty("availableBalance") BigDecimal availableBalance,
            @JsonProperty("assets") List<BinanceAccountAsset> assets) {
    }

    /**
     * {@code unRealizedProfit} (capital R) is Binance's actual field name on
     * {@code /positionRisk} — a different casing from {@code /account}'s
     * {@code unrealizedProfit} (a real, historically-confirmed inconsistency
     * between these two endpoint families). {@link JsonAlias} accepts both
     * defensively in case that casing ever changes underneath us.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BinancePositionResponse(
            @JsonProperty("positionAmt") BigDecimal positionAmt,
            @JsonProperty("entryPrice") BigDecimal entryPrice,
            @JsonAlias("unrealizedProfit") @JsonProperty("unRealizedProfit") BigDecimal unrealizedProfit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BinanceOrderResponse(
            @JsonProperty("orderId") Long orderId,
            @JsonProperty("status") String status,
            @JsonProperty("avgPrice") BigDecimal avgPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BinanceErrorResponse(
            @JsonProperty("code") Long code,
            @JsonProperty("msg") String msg) {
    }
}
