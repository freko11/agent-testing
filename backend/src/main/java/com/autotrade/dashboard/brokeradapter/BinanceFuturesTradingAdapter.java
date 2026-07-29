package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.ticker.AssetType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
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
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Real {@link BrokerAdapter} for Binance's USD&#9328;-M Futures Testnet trading
 * API (E4-F3-S1) — {@code https://testnet.binancefuture.com} for {@code
 * PAPER}, {@code https://fapi.binance.com} for {@code LIVE}. Futures
 * Testnet, not Spot Testnet, was the deliberate choice (confirmed with the
 * user): E4-F3-S2's leveraged-order requirement needs real margin/leverage
 * support Spot doesn't have, so both stories target the same base API. This
 * intentionally diverges from indicators being computed off real spot
 * prices ({@code marketdata.BinanceMarketDataClient}) — the same accepted
 * paper/live price divergence {@code application-paper.properties} already
 * documents for Alpaca, extended one step further to trading itself.
 *
 * <p>Every request is HMAC-SHA256 signed per Binance's SIGNED-endpoint
 * convention: params (including {@code timestamp}/{@code recvWindow}) are
 * concatenated into a canonical {@code key=value&...} query string, signed
 * with the API secret, and the hex signature appended as the final param;
 * the {@code X-MBX-APIKEY} header carries the API key. <b>The signed query
 * string is sent as-is via {@link RestClient}'s literal-string {@code
 * uri(String)} overload — never rebuilt through a {@code UriBuilder}/{@code
 * UriComponentsBuilder} lambda</b>, since re-encoding an already-signed
 * query string (so the string that was signed no longer matches the string
 * actually sent) is the single most common real-world Binance-signing bug.
 * This is safe here because every param value this adapter sends (symbols,
 * timestamps, the hex signature itself) is plain alphanumeric — nothing
 * that would ever need percent-encoding in the first place.
 *
 * <p>{@code placeOrder}/{@code getOrderStatus}/{@code cancelOrder} are
 * deliberately not implemented yet (confirmed with the user, a narrower
 * scope than {@code AlpacaTradingAdapter}'s E4-F2-S1 "build the full
 * interface ahead of need") — Binance Futures has no single-call bracket
 * order the way Alpaca does, and leverage/TP-SL construction is explicitly
 * E4-F3-S2's scope. Calling any of them now throws a clear, documented
 * {@link BrokerAdapterException} rather than rushing that design or risking
 * an order with no protective stop-loss attached. {@code getAccountStatus}/
 * {@code getPosition} are fully real, against {@code GET /fapi/v3/account}
 * and {@code GET /fapi/v3/positionRisk} respectively.
 */
public class BinanceFuturesTradingAdapter implements BrokerAdapter {

    private static final String API_KEY_HEADER = "X-MBX-APIKEY";
    private static final long RECV_WINDOW_MILLIS = 5_000;

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
        throw deferredToNextStory("placeOrder");
    }

    @Override
    public Optional<BrokerOrderResult> getOrderStatus(String symbol, String clientOrderId, TradingMode mode) {
        throw deferredToNextStory("getOrderStatus");
    }

    @Override
    public BrokerOrderResult cancelOrder(String symbol, String clientOrderId, TradingMode mode) {
        throw deferredToNextStory("cancelOrder");
    }

    @Override
    public Optional<BrokerPosition> getPosition(String symbol, TradingMode mode) {
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

    private <T> T signedGet(RestClient restClient, Credentials creds, String path, Map<String, String> params, Class<T> responseType) {
        params.put("timestamp", String.valueOf(clock.millis()));
        params.put("recvWindow", String.valueOf(RECV_WINDOW_MILLIS));
        String signedQuery = signedQuery(params, creds.apiSecret());

        try {
            return restClient.get()
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

    private static BrokerAdapterException deferredToNextStory(String method) {
        return new BrokerAdapterException(Broker.BINANCE,
                method + " is not yet implemented on BinanceFuturesTradingAdapter — bracket/leverage order "
                        + "construction is E4-F3-S2's scope, not this adapter's current "
                        + "(E4-F3-S1, getAccountStatus/getPosition only) scope.");
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
    private record BinanceErrorResponse(
            @JsonProperty("code") Long code,
            @JsonProperty("msg") String msg) {
    }
}
