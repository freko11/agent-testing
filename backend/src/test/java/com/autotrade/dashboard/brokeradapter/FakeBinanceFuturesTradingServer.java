package com.autotrade.dashboard.brokeradapter;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * A minimal in-memory fake of Binance's USD&#9328;-M Futures Testnet trading
 * API — enough of {@code GET /fapi/v3/account} and {@code GET
 * /fapi/v3/positionRisk} to satisfy {@link BrokerAdapterContractTest}'s two
 * still-enabled assertions for E4-F3-S1's narrower scope (see {@link
 * BinanceFuturesTradingAdapterContractTest} for exactly which tests are
 * disabled and why). Asserts every request carries {@code X-MBX-APIKEY} and
 * a {@code signature} query param — a concrete check that signing actually
 * happened, not just trusted. Mirrors {@code FakeAlpacaTradingServer}'s
 * custom-{@code ResponseCreator} approach so call order isn't fixed.
 */
class FakeBinanceFuturesTradingServer {

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
        if (UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("signature") == null) {
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
            String symbol = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("symbol");
            if (symbol != null && symbol.endsWith("-NOACTIVITY")) {
                return jsonResponse(HttpStatus.OK,
                        "[{\"symbol\":\"" + symbol + "\",\"positionAmt\":\"0\",\"entryPrice\":\"0.0\",\"unRealizedProfit\":\"0.0\"}]");
            }
            return jsonResponse(HttpStatus.OK,
                    "[{\"symbol\":\"" + symbol + "\",\"positionAmt\":\"0.500\",\"entryPrice\":\"60000.0\",\"unRealizedProfit\":\"12.5\"}]");
        }
        return jsonResponse(HttpStatus.NOT_FOUND, "{\"code\":-1121,\"msg\":\"unhandled fake route: " + method + " " + path + "\"}");
    }

    private ClientHttpResponse jsonResponse(HttpStatus status, String body) {
        MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response;
    }
}
