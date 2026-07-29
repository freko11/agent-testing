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
 * {@code DELETE /fapi/v1/order} (E4-F3-S2) tracked via an in-memory map keyed
 * by {@code newClientOrderId}/{@code origClientOrderId} — enough to satisfy
 * {@link BrokerAdapterContractTest}'s full shared suite now that {@code
 * placeOrder}/{@code getOrderStatus}/{@code cancelOrder} are real. A
 * {@code POST /fapi/v1/order} with {@code type=MARKET} fills immediately
 * (this fake has no concept of a resting order); any other type (the
 * {@code STOP_MARKET}/{@code TAKE_PROFIT_MARKET} exit legs) is stored as
 * {@code NEW}. Asserts every request carries {@code X-MBX-APIKEY} and a
 * {@code signature} query param — a concrete check that signing actually
 * happened, not just trusted. Mirrors {@code FakeAlpacaTradingServer}'s
 * custom-{@code ResponseCreator} approach so call order isn't fixed;
 * adapter-level failure-path scenarios (rejections, rate limits, leg
 * failures) belong in {@link BinanceFuturesTradingAdapterTest}'s own
 * {@code MockRestServiceServer} expectations instead of here, same split as
 * Alpaca's fake/adapter-test pair.
 */
class FakeBinanceFuturesTradingServer {

    private static final String ENTRY_FILL_PRICE = "60000.00";

    private final Map<String, StoredOrder> ordersByClientOrderId = new HashMap<>();
    private long nextOrderId = 1000;

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
        return jsonResponse(HttpStatus.NOT_FOUND, "{\"code\":-1121,\"msg\":\"unhandled fake route: " + method + " " + path + "\"}");
    }

    private ClientHttpResponse handlePlaceOrder(MultiValueMap<String, String> query) {
        String clientOrderId = query.getFirst("newClientOrderId");
        String type = query.getFirst("type");
        boolean isEntry = "MARKET".equals(type);
        String status = isEntry ? "FILLED" : "NEW";
        String avgPrice = isEntry ? ENTRY_FILL_PRICE : "0";
        long orderId = nextOrderId++;
        ordersByClientOrderId.put(clientOrderId, new StoredOrder(orderId, clientOrderId, status, avgPrice));
        return jsonResponse(HttpStatus.OK, orderJson(orderId, clientOrderId, status, avgPrice));
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

    private String orderJson(long orderId, String clientOrderId, String status, String avgPrice) {
        return "{\"orderId\":" + orderId + ",\"clientOrderId\":\"" + clientOrderId + "\",\"status\":\"" + status
                + "\",\"avgPrice\":\"" + avgPrice + "\"}";
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
}
