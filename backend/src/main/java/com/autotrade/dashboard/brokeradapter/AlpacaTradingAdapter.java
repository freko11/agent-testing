package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.EntryOrderType;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Real {@link BrokerAdapter} for Alpaca's stock paper/live trading API
 * (E4-F2-S1) — {@code https://paper-api.alpaca.markets} /
 * {@code https://api.alpaca.markets}, distinct from {@code
 * marketdata.AlpacaMarketDataClient}'s read-only {@code data.alpaca.markets}
 * base URL. Credentials come from {@link BrokerCredentialService}, never
 * plain config, per E1-F3-S1's encrypted/rotation-eligible store.
 *
 * <p>Alpaca order status vocabulary is mapped onto this codebase's {@link
 * OrderStatus}: {@code new/accepted/pending_new/accepted_for_bidding/
 * calculated/held/stopped/pending_cancel/pending_replace/replaced} ->
 * {@code SUBMITTED}; {@code partially_filled} -> {@code PARTIALLY_FILLED};
 * {@code filled} -> {@code FILLED}; {@code canceled} -> {@code CANCELLED};
 * {@code expired/done_for_day/suspended} -> {@code CANCELLED} (provisional —
 * this enum has no distinct "expired" value); {@code rejected} -> {@code
 * REJECTED}.
 *
 * <p>Per {@link BrokerAdapter}'s contract, only {@link
 * BrokerAdapterTransientException} (5xx/connectivity) and {@link
 * BrokerAdapterRateLimitedException} (429) are thrown for transport faults —
 * everything else that's a genuine business outcome (insufficient buying
 * power, an already-terminal order) is returned as a normal result value.
 * Not a Spring bean directly — see {@code BrokerAdapterConfig}, which wraps
 * this in {@link RetryingBrokerAdapter} for the actual exposed bean, mirroring
 * how {@code MockBrokerAdapter} is deliberately not one either (for a
 * different reason: this adapter is real, just not the thing that should be
 * directly injectable).
 */
public class AlpacaTradingAdapter implements BrokerAdapter {

    private static final String API_KEY_HEADER = "APCA-API-KEY-ID";
    private static final String API_SECRET_HEADER = "APCA-API-SECRET-KEY";
    private static final long DUPLICATE_CLIENT_ORDER_ID_CODE = 40010001L;

    private final Map<TradingMode, RestClient> restClientsByMode;
    private final BrokerCredentialService credentialService;
    private final Clock clock;
    private final ObjectMapper errorBodyMapper = new ObjectMapper();

    public AlpacaTradingAdapter(Map<TradingMode, RestClient> restClientsByMode,
                                 BrokerCredentialService credentialService,
                                 Clock clock) {
        this.restClientsByMode = restClientsByMode;
        this.credentialService = credentialService;
        this.clock = clock;
    }

    @Override
    public AssetType supportedAssetType() {
        return AssetType.STOCK;
    }

    @Override
    public Broker broker() {
        return Broker.ALPACA;
    }

    @Override
    public BrokerOrderResult placeOrder(BrokerOrderRequest request, TradingMode mode) {
        validateStockNoLeverage(request);
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);

        try {
            AlpacaOrderResponse response = restClient.post()
                    .uri("/v2/orders")
                    .header(API_KEY_HEADER, creds.apiKey())
                    .header(API_SECRET_HEADER, creds.apiSecret())
                    .body(toOrderRequestBody(request))
                    .retrieve()
                    .body(AlpacaOrderResponse.class);
            return toResult(response);
        } catch (HttpClientErrorException.Forbidden e) {
            return rejectedResult(request.clientOrderId(), errorMessage(e, "Rejected by Alpaca"));
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            if (isDuplicateClientOrderId(e)) {
                return getOrderStatus(request.clientOrderId(), mode)
                        .orElseThrow(() -> new BrokerAdapterException(Broker.ALPACA,
                                "Alpaca reported client_order_id '" + request.clientOrderId()
                                        + "' as a duplicate, but no matching order was found on replay"));
            }
            throw new BrokerAdapterException(Broker.ALPACA,
                    "Alpaca rejected the order request: " + errorMessage(e, e.getMessage()));
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BrokerAdapterRateLimitedException(Broker.ALPACA, retryAfterSeconds(e));
        } catch (RestClientException e) {
            throw transientOrFatal(e);
        }
    }

    @Override
    public Optional<BrokerOrderResult> getOrderStatus(String clientOrderId, TradingMode mode) {
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);

        try {
            AlpacaOrderResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/orders:by_client_order_id")
                            .queryParam("client_order_id", clientOrderId)
                            .build())
                    .header(API_KEY_HEADER, creds.apiKey())
                    .header(API_SECRET_HEADER, creds.apiSecret())
                    .retrieve()
                    .body(AlpacaOrderResponse.class);
            return Optional.ofNullable(response).map(this::toResult);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BrokerAdapterRateLimitedException(Broker.ALPACA, retryAfterSeconds(e));
        } catch (RestClientException e) {
            throw transientOrFatal(e);
        }
    }

    @Override
    public Optional<BrokerPosition> getPosition(String symbol, TradingMode mode) {
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);

        try {
            AlpacaPositionResponse response = restClient.get()
                    .uri("/v2/positions/{symbol}", symbol)
                    .header(API_KEY_HEADER, creds.apiKey())
                    .header(API_SECRET_HEADER, creds.apiSecret())
                    .retrieve()
                    .body(AlpacaPositionResponse.class);
            return Optional.ofNullable(response).map(r -> new BrokerPosition(
                    symbol, AssetType.STOCK, r.qty(), r.avgEntryPrice(), r.unrealizedPl(), clock.instant()));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BrokerAdapterRateLimitedException(Broker.ALPACA, retryAfterSeconds(e));
        } catch (RestClientException e) {
            throw transientOrFatal(e);
        }
    }

    @Override
    public BrokerOrderResult cancelOrder(String clientOrderId, TradingMode mode) {
        Optional<BrokerOrderResult> existing = getOrderStatus(clientOrderId, mode);
        if (existing.isEmpty()) {
            return new BrokerOrderResult(clientOrderId, null, OrderStatus.FAILED, null, "Unknown clientOrderId", clock.instant());
        }
        BrokerOrderResult current = existing.get();
        if (isTerminal(current.status())) {
            // Already FILLED/CANCELLED/REJECTED/FAILED — idempotent no-op, same convention as MockBrokerAdapter.
            return current;
        }

        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);
        try {
            restClient.delete()
                    .uri("/v2/orders/{orderId}", current.brokerOrderId())
                    .header(API_KEY_HEADER, creds.apiKey())
                    .header(API_SECRET_HEADER, creds.apiSecret())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.UnprocessableEntity e) {
            // Order became not-cancelable between our status check and this call (e.g. just filled) — idempotent no-op.
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BrokerAdapterRateLimitedException(Broker.ALPACA, retryAfterSeconds(e));
        } catch (RestClientException e) {
            throw transientOrFatal(e);
        }

        // A 204 from DELETE means "cancellation accepted," not "done" — re-fetch for the authoritative status.
        return getOrderStatus(clientOrderId, mode).orElse(current);
    }

    @Override
    public BrokerAccountStatus getAccountStatus(TradingMode mode) {
        Credentials creds = resolveCredential(mode);
        RestClient restClient = restClientFor(mode);

        try {
            AlpacaAccountResponse response = restClient.get()
                    .uri("/v2/account")
                    .header(API_KEY_HEADER, creds.apiKey())
                    .header(API_SECRET_HEADER, creds.apiSecret())
                    .retrieve()
                    .body(AlpacaAccountResponse.class);
            if (response == null) {
                throw new BrokerAdapterException(Broker.ALPACA, "Alpaca returned an empty account response");
            }
            String currency = response.currency() != null ? response.currency() : "USD";
            List<BrokerAccountStatus.AssetBalance> balances =
                    List.of(new BrokerAccountStatus.AssetBalance(currency, response.cash(), BigDecimal.ZERO));
            return new BrokerAccountStatus(
                    Broker.ALPACA, mode, balances, response.equity(), response.buyingPower(), clock.instant());
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new BrokerAdapterRateLimitedException(Broker.ALPACA, retryAfterSeconds(e));
        } catch (RestClientException e) {
            throw transientOrFatal(e);
        }
    }

    private void validateStockNoLeverage(BrokerOrderRequest request) {
        if (request.assetType() != AssetType.STOCK) {
            throw new BrokerAdapterException(Broker.ALPACA,
                    "AlpacaTradingAdapter only supports STOCK orders, got " + request.assetType());
        }
        if (request.leverage() != null && request.leverage().compareTo(BigDecimal.ONE) != 0) {
            throw new BrokerAdapterException(Broker.ALPACA,
                    "Stock orders must not carry leverage (got " + request.leverage() + ")");
        }
    }

    private Credentials resolveCredential(TradingMode mode) {
        BrokerCredential stored = credentialService.find(Broker.ALPACA, mode)
                .orElseThrow(() -> new BrokerAdapterException(Broker.ALPACA,
                        "No Alpaca trading credential configured for mode " + mode));
        BrokerCredentialService.DecryptedCredential decrypted = credentialService.readDecrypted(stored);
        return new Credentials(decrypted.apiKey(), decrypted.apiSecret());
    }

    private RestClient restClientFor(TradingMode mode) {
        RestClient restClient = restClientsByMode.get(mode);
        if (restClient == null) {
            throw new BrokerAdapterException(Broker.ALPACA, "No Alpaca trading RestClient configured for mode " + mode);
        }
        return restClient;
    }

    private static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.FILLED || status == OrderStatus.CANCELLED
                || status == OrderStatus.REJECTED || status == OrderStatus.FAILED;
    }

    private RuntimeException transientOrFatal(RestClientException e) {
        if (e instanceof HttpServerErrorException || e instanceof ResourceAccessException) {
            return new BrokerAdapterTransientException(Broker.ALPACA, "Alpaca request failed: " + e.getMessage(), e);
        }
        return new BrokerAdapterException(Broker.ALPACA, "Alpaca request failed: " + e.getMessage(), e);
    }

    private static Long retryAfterSeconds(HttpClientErrorException.TooManyRequests e) {
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

    private boolean isDuplicateClientOrderId(HttpClientErrorException.UnprocessableEntity e) {
        AlpacaErrorResponse error = parseError(e);
        if (error == null) {
            return false;
        }
        if (error.code() != null && error.code() == DUPLICATE_CLIENT_ORDER_ID_CODE) {
            return true;
        }
        return error.message() != null && error.message().toLowerCase().contains("client_order_id");
    }

    private String errorMessage(HttpStatusCodeException e, String fallback) {
        AlpacaErrorResponse error = parseError(e);
        return error != null && error.message() != null ? error.message() : fallback;
    }

    private AlpacaErrorResponse parseError(HttpStatusCodeException e) {
        try {
            return errorBodyMapper.readValue(e.getResponseBodyAsString(), AlpacaErrorResponse.class);
        } catch (Exception parseFailure) {
            return null;
        }
    }

    private AlpacaOrderRequestBody toOrderRequestBody(BrokerOrderRequest request) {
        boolean isLimit = request.entryOrderType() == EntryOrderType.LIMIT;
        return new AlpacaOrderRequestBody(
                request.symbol(),
                request.quantity().toPlainString(),
                request.side() == OrderSide.BUY ? "buy" : "sell",
                isLimit ? "limit" : "market",
                "day",
                "bracket",
                request.clientOrderId(),
                isLimit ? request.entryLimitPrice().toPlainString() : null,
                new AlpacaBracketLeg(request.takeProfitPrice().toPlainString()),
                new AlpacaStopLeg(request.stopLossPrice().toPlainString()));
    }

    private BrokerOrderResult toResult(AlpacaOrderResponse response) {
        OrderStatus status = mapStatus(response.status());
        BigDecimal filledPrice = (status == OrderStatus.FILLED || status == OrderStatus.PARTIALLY_FILLED)
                ? response.filledAvgPrice() : null;
        String rejectionReason = status == OrderStatus.REJECTED ? "Rejected by Alpaca" : null;
        return new BrokerOrderResult(response.clientOrderId(), response.id(), status, filledPrice, rejectionReason, clock.instant());
    }

    private BrokerOrderResult rejectedResult(String clientOrderId, String reason) {
        return new BrokerOrderResult(clientOrderId, null, OrderStatus.REJECTED, null, reason, clock.instant());
    }

    private static OrderStatus mapStatus(String alpacaStatus) {
        if (alpacaStatus == null) {
            return OrderStatus.SUBMITTED;
        }
        return switch (alpacaStatus) {
            case "filled" -> OrderStatus.FILLED;
            case "partially_filled" -> OrderStatus.PARTIALLY_FILLED;
            case "canceled", "expired", "done_for_day", "suspended" -> OrderStatus.CANCELLED;
            case "rejected" -> OrderStatus.REJECTED;
            // new, accepted, pending_new, accepted_for_bidding, calculated, held, stopped, pending_cancel,
            // pending_replace, replaced — all still in flight.
            default -> OrderStatus.SUBMITTED;
        };
    }

    private record Credentials(String apiKey, String apiSecret) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record AlpacaOrderRequestBody(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("qty") String qty,
            @JsonProperty("side") String side,
            @JsonProperty("type") String type,
            @JsonProperty("time_in_force") String timeInForce,
            @JsonProperty("order_class") String orderClass,
            @JsonProperty("client_order_id") String clientOrderId,
            @JsonProperty("limit_price") String limitPrice,
            @JsonProperty("take_profit") AlpacaBracketLeg takeProfit,
            @JsonProperty("stop_loss") AlpacaStopLeg stopLoss) {
    }

    private record AlpacaBracketLeg(@JsonProperty("limit_price") String limitPrice) {
    }

    private record AlpacaStopLeg(@JsonProperty("stop_price") String stopPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlpacaOrderResponse(
            @JsonProperty("id") String id,
            @JsonProperty("client_order_id") String clientOrderId,
            @JsonProperty("status") String status,
            @JsonProperty("filled_avg_price") BigDecimal filledAvgPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlpacaPositionResponse(
            @JsonProperty("qty") BigDecimal qty,
            @JsonProperty("avg_entry_price") BigDecimal avgEntryPrice,
            @JsonProperty("unrealized_pl") BigDecimal unrealizedPl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlpacaAccountResponse(
            @JsonProperty("cash") BigDecimal cash,
            @JsonProperty("equity") BigDecimal equity,
            @JsonProperty("buying_power") BigDecimal buyingPower,
            @JsonProperty("currency") String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlpacaErrorResponse(
            @JsonProperty("code") Long code,
            @JsonProperty("message") String message) {
    }
}
