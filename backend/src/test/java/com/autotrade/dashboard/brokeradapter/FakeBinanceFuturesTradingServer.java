package com.autotrade.dashboard.brokeradapter;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A minimal in-memory fake of Binance's USD&#9328;-M Futures Testnet trading
 * API — {@code GET /fapi/v3/account} and {@code GET /fapi/v3/positionRisk}
 * (E4-F3-S1), plus {@code POST /fapi/v1/leverage}, {@code POST}/{@code GET}/
 * {@code DELETE /fapi/v1/order} for the MARKET entry leg (E4-F3-S2), and
 * {@code POST}/{@code GET /fapi/v1/algoOrder} for the two conditional exit
 * legs (E4-F3-S3 — see {@code BinanceFuturesTradingAdapter}'s class Javadoc
 * for why exit legs moved off {@code /fapi/v1/order}) — tracked via
 * in-memory maps keyed by {@code newClientOrderId}/{@code clientAlgoId} —
 * enough to satisfy {@link BrokerAdapterContractTest}'s full shared suite
 * now that {@code placeOrder}/{@code getOrderStatus}/{@code cancelOrder}
 * are real. A {@code POST /fapi/v1/order} always fills immediately (this
 * fake has no concept of a resting order — every order it now sees at this
 * endpoint is a MARKET entry, since exit legs moved to {@code
 * /fapi/v1/algoOrder}); a {@code POST /fapi/v1/algoOrder} is stored as
 * {@code WORKING}. Asserts every request carries {@code X-MBX-APIKEY} and a
 * {@code signature} query param — a concrete check that signing actually
 * happened, not just trusted. Mirrors {@code FakeAlpacaTradingServer}'s
 * custom-{@code ResponseCreator} approach so call order isn't fixed;
 * adapter-level failure-path scenarios (rejections, rate limits, leg
 * failures) belong in {@link BinanceFuturesTradingAdapterTest}'s own
 * {@code MockRestServiceServer} expectations instead of here, same split as
 * Alpaca's fake/adapter-test pair. {@code DELETE /fapi/v1/algoOrder} is
 * deliberately unhandled (falls through to the generic 404 route) — E4-F3-S3
 * scoped {@code cancelOrder} to the entry leg only, same as before this
 * story (confirmed: by the time exit legs exist, the entry has already
 * filled and is no longer cancelable, so there was never a gap to close).
 */
class FakeBinanceFuturesTradingServer {

    private static final String ENTRY_FILL_PRICE = "60000.00";

    private final Map<String, StoredOrder> ordersByClientOrderId = new HashMap<>();
    private final Map<String, StoredAlgoOrder> algoOrdersByClientAlgoId = new HashMap<>();
    private long nextOrderId = 1000;
    private long nextAlgoId = 2000;

    RestClient buildRestClient(String baseUrl) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        MockRestServiceServer.bindTo(builder).build()
                .expect(ExpectedCount.manyTimes(), request -> { })
                .andRespond(this::respond);
        return builder.build();
    }

    private ClientHttpResponse respond(ClientHttpRequest request) throws IOException {
        if (request.getHeaders().getFirst("X-MBX-APIKEY") == null) {
            return jsonResponse(HttpStatus.UNAUTHORIZED, "{\"code\":-2015,\"msg\":\"Invalid API-key, IP, or permissions for action.\"}");
        }
        URI uri = request.getURI();
        MultiValueMap<String, String> query = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        if (query.getFirst("signature") == null) {
            return jsonResponse(HttpStatus.BAD_REQUEST, "{\"code\":-1022,\"msg\":\"Signature for this request is not valid.\"}");
        }

        HttpMethod method = request.getMethod();
        String path = uri.getPath();

        if (HttpMethod.GET.equals(method) && "/fapi/v3/account".equals(path)) {
            return jsonResponse(HttpStatus.OK,
                    "{\"totalMarginBalance\":\"126.72469206\",\"availableBalance\":\"100.00000000\","
                            + "\"assets\":[{\"asset\":\"USDT\",\"walletBalance\":\"126.72469206\",\"availableBalance\":\"100.00000000\"}]}");
        }
        if (HttpMethod.GET.equals(method) && "/fapi/v3/positionRisk".equals(path)) {
            String symbol = query.getFirst("symbol");
            if (symbol != null && symbol.endsWith("-NOACTIVITY")) {
                return jsonResponse(HttpStatus.OK,
                        "[{\"symbol\":\"" + symbol + "\",\"positionAmt\":\"0\",\"entryPrice\":\"0.0\",\"unRealizedProfit\":\"0.0\"}]");
            }
            return jsonResponse(HttpStatus.OK,
                    "[{\"symbol\":\"" + symbol + "\",\"positionAmt\":\"0.500\",\"entryPrice\":\"60000.0\",\"unRealizedProfit\":\"12.5\"}]");
        }
        if (HttpMethod.POST.equals(method) && "/fapi/v1/leverage".equals(path)) {
            String symbol = query.getFirst("symbol");
            String leverage = query.getFirst("leverage");
            return jsonResponse(HttpStatus.OK,
                    "{\"leverage\":" + leverage + ",\"maxNotionalValue\":\"1000000\",\"symbol\":\"" + symbol + "\"}");
        }
        if (HttpMethod.POST.equals(method) && "/fapi/v1/order".equals(path)) {
            return handlePlaceOrder(query);
        }
        if (HttpMethod.GET.equals(method) && "/fapi/v1/order".equals(path)) {
            return handleGetOrder(query);
        }
        if (HttpMethod.DELETE.equals(method) && "/fapi/v1/order".equals(path)) {
            return handleDeleteOrder(query);
        }
        if (HttpMethod.POST.equals(method) && "/fapi/v1/algoOrder".equals(path)) {
            return handlePlaceAlgoOrder(query);
        }
        if (HttpMethod.GET.equals(method) && "/fapi/v1/algoOrder".equals(path)) {
            return handleGetAlgoOrder(query);
        }
        return jsonResponse(HttpStatus.NOT_FOUND, "{\"code\":-1121,\"msg\":\"unhandled fake route: " + method + " " + path + "\"}");
    }

    private ClientHttpResponse handlePlaceOrder(MultiValueMap<String, String> query) {
        // Only the MARKET entry leg reaches this endpoint now — exit legs place via
        // /fapi/v1/algoOrder instead (E4-F3-S3) — so it always fills immediately.
        String clientOrderId = query.getFirst("newClientOrderId");
        long orderId = nextOrderId++;
        ordersByClientOrderId.put(clientOrderId, new StoredOrder(orderId, clientOrderId, "FILLED", ENTRY_FILL_PRICE));
        return jsonResponse(HttpStatus.OK, orderJson(orderId, clientOrderId, "FILLED", ENTRY_FILL_PRICE));
    }

    private ClientHttpResponse handleGetOrder(MultiValueMap<String, String> query) {
        StoredOrder order = ordersByClientOrderId.get(query.getFirst("origClientOrderId"));
        if (order == null) {
            return jsonResponse(HttpStatus.BAD_REQUEST, "{\"code\":-2013,\"msg\":\"Order does not exist.\"}");
        }
        return jsonResponse(HttpStatus.OK, orderJson(order.orderId, order.clientOrderId, order.status, order.avgPrice));
    }

    private ClientHttpResponse handleDeleteOrder(MultiValueMap<String, String> query) {
        StoredOrder order = ordersByClientOrderId.get(query.getFirst("origClientOrderId"));
        if (order == null) {
            return jsonResponse(HttpStatus.BAD_REQUEST, "{\"code\":-2011,\"msg\":\"Unknown order sent.\"}");
        }
        order.status = "CANCELED";
        return jsonResponse(HttpStatus.OK, orderJson(order.orderId, order.clientOrderId, order.status, order.avgPrice));
    }

    private ClientHttpResponse handlePlaceAlgoOrder(MultiValueMap<String, String> query) {
        String clientAlgoId = query.getFirst("clientAlgoId");
        long algoId = nextAlgoId++;
        algoOrdersByClientAlgoId.put(clientAlgoId, new StoredAlgoOrder(algoId, clientAlgoId, "WORKING"));
        return jsonResponse(HttpStatus.OK, algoOrderJson(algoId, "WORKING"));
    }

    private ClientHttpResponse handleGetAlgoOrder(MultiValueMap<String, String> query) {
        StoredAlgoOrder algoOrder = algoOrdersByClientAlgoId.get(query.getFirst("clientAlgoId"));
        if (algoOrder == null) {
            return jsonResponse(HttpStatus.BAD_REQUEST, "{\"code\":-2013,\"msg\":\"Order does not exist.\"}");
        }
        return jsonResponse(HttpStatus.OK, algoOrderJson(algoOrder.algoId, algoOrder.algoStatus));
    }

    private String orderJson(long orderId, String clientOrderId, String status, String avgPrice) {
        return "{\"orderId\":" + orderId + ",\"clientOrderId\":\"" + clientOrderId + "\",\"status\":\"" + status
                + "\",\"avgPrice\":\"" + avgPrice + "\"}";
    }

    private String algoOrderJson(long algoId, String algoStatus) {
        return "{\"algoId\":" + algoId + ",\"algoStatus\":\"" + algoStatus + "\"}";
    }

    private ClientHttpResponse jsonResponse(HttpStatus status, String body) {
        MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }

    private static final class StoredOrder {
        private final long orderId;
        private final String clientOrderId;
        private String status;
        private final String avgPrice;

        private StoredOrder(long orderId, String clientOrderId, String status, String avgPrice) {
            this.orderId = orderId;
            this.clientOrderId = clientOrderId;
            this.status = status;
            this.avgPrice = avgPrice;
        }
    }

    private static final class StoredAlgoOrder {
        private final long algoId;
        private final String clientAlgoId;
        private final String algoStatus;

        private StoredAlgoOrder(long algoId, String clientAlgoId, String algoStatus) {
            this.algoId = algoId;
            this.clientAlgoId = clientAlgoId;
            this.algoStatus = algoStatus;
        }
    }
}
