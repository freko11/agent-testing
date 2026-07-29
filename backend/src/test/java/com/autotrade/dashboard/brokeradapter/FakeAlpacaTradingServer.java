package com.autotrade.dashboard.brokeradapter;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A minimal in-memory fake of Alpaca's trading API — enough of {@code
 * POST /v2/orders}, {@code GET /v2/orders:by_client_order_id}, {@code
 * DELETE /v2/orders/{id}}, {@code GET /v2/positions/{symbol}}, and {@code
 * GET /v2/account} to satisfy {@link BrokerAdapterContractTest}'s shared
 * suite end-to-end over real HTTP-shaped request/response objects (via
 * {@link MockRestServiceServer}'s custom-{@code ResponseCreator} escape
 * hatch), without depending on call order the way a fixed sequence of
 * {@code .expect(...)} calls would. Positions always 404 — the shared suite
 * only ever asks about a symbol with no activity, so a real
 * fill-to-position pipeline isn't needed here (unlike {@code
 * MockBrokerAdapter}, which does need one for its own more detailed tests).
 */
class FakeAlpacaTradingServer {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, FakeOrder> ordersByClientOrderId = new HashMap<>();
    private final AtomicLong brokerOrderSequence = new AtomicLong(1);

    RestClient buildRestClient(String baseUrl) {
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        MockRestServiceServer.bindTo(builder).build()
                .expect(ExpectedCount.manyTimes(), request -> { })
                .andRespond(this::respond);
        return builder.build();
    }

    private ClientHttpResponse respond(ClientHttpRequest request) throws IOException {
        HttpMethod method = request.getMethod();
        URI uri = request.getURI();
        String path = uri.getPath();

        if (HttpMethod.POST.equals(method) && "/v2/orders".equals(path)) {
            return handleCreateOrder((MockClientHttpRequest) request);
        }
        if (HttpMethod.GET.equals(method) && "/v2/orders:by_client_order_id".equals(path)) {
            return handleGetByClientOrderId(uri);
        }
        if (HttpMethod.DELETE.equals(method) && path.startsWith("/v2/orders/")) {
            return handleCancel(path.substring("/v2/orders/".length()));
        }
        if (HttpMethod.GET.equals(method) && path.startsWith("/v2/positions/")) {
            return jsonResponse(HttpStatus.NOT_FOUND, "{\"code\":40410000,\"message\":\"position does not exist\"}");
        }
        if (HttpMethod.GET.equals(method) && "/v2/account".equals(path)) {
            return jsonResponse(HttpStatus.OK,
                    "{\"cash\":\"98000.00\",\"equity\":\"100500.00\",\"buying_power\":\"196000.00\",\"currency\":\"USD\"}");
        }
        return jsonResponse(HttpStatus.NOT_FOUND,
                "{\"code\":40410000,\"message\":\"unhandled fake route: " + method + " " + path + "\"}");
    }

    private ClientHttpResponse handleCreateOrder(MockClientHttpRequest request) throws IOException {
        JsonNode body = mapper.readTree(request.getBodyAsString());
        String clientOrderId = body.path("client_order_id").asText();

        if (ordersByClientOrderId.containsKey(clientOrderId)) {
            return jsonResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                    "{\"code\":40010001,\"message\":\"client_order_id must be unique\"}");
        }

        String brokerOrderId = "FAKE-" + brokerOrderSequence.getAndIncrement();
        FakeOrder order = new FakeOrder(brokerOrderId, clientOrderId, "new");
        ordersByClientOrderId.put(clientOrderId, order);
        return jsonResponse(HttpStatus.OK, orderJson(order));
    }

    private ClientHttpResponse handleGetByClientOrderId(URI uri) throws IOException {
        String clientOrderId = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("client_order_id");
        FakeOrder order = clientOrderId == null ? null : ordersByClientOrderId.get(clientOrderId);
        if (order == null) {
            return jsonResponse(HttpStatus.NOT_FOUND, "{\"code\":40410000,\"message\":\"order not found\"}");
        }
        return jsonResponse(HttpStatus.OK, orderJson(order));
    }

    private ClientHttpResponse handleCancel(String brokerOrderId) throws IOException {
        FakeOrder order = ordersByClientOrderId.values().stream()
                .filter(o -> o.brokerOrderId.equals(brokerOrderId))
                .findFirst()
                .orElse(null);
        if (order == null) {
            return jsonResponse(HttpStatus.NOT_FOUND, "{\"code\":40410000,\"message\":\"order not found\"}");
        }
        if ("filled".equals(order.status) || "canceled".equals(order.status)) {
            return jsonResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                    "{\"code\":42210000,\"message\":\"order status is not cancelable\"}");
        }
        order.status = "canceled";
        return new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT);
    }

    private String orderJson(FakeOrder order) {
        String filledAvgPrice = "filled".equals(order.status) ? "\"200\"" : "null";
        return "{\"id\":\"" + order.brokerOrderId + "\",\"client_order_id\":\"" + order.clientOrderId
                + "\",\"status\":\"" + order.status + "\",\"filled_avg_price\":" + filledAvgPrice + "}";
    }

    private ClientHttpResponse jsonResponse(HttpStatus status, String body) {
        MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }

    private static final class FakeOrder {
        private final String brokerOrderId;
        private final String clientOrderId;
        private String status;

        private FakeOrder(String brokerOrderId, String clientOrderId, String status) {
            this.brokerOrderId = brokerOrderId;
            this.clientOrderId = clientOrderId;
            this.status = status;
        }
    }
}
