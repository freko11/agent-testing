package com.autotrade.dashboard.marketdata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Proves the JSON->Candle mapping (including string-typed numerics and
 * epoch-millis timestamps) for E2-F1-S1's Binance client, and that no auth
 * header is sent since klines are a fully public endpoint.
 */
class BinanceMarketDataClientTest {

    private MockRestServiceServer server;
    private BinanceMarketDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BinanceMarketDataClient(builder.build());
    }

    @Test
    void fetchRecentCandles_parsesKlinesWithoutAuthHeader() throws IOException {
        server.expect(requestToUriTemplate(
                        "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1d&limit=2"))
                .andExpect(request -> assertNull(request.getHeaders().getFirst("APCA-API-KEY-ID")))
                .andRespond(withSuccess(fixture("binance-klines-sample.json"), MediaType.APPLICATION_JSON));

        List<Candle> candles = client.fetchRecentCandles("BTCUSDT", 2);

        assertEquals(2, candles.size());
        assertEquals(0, candles.get(0).close().compareTo(new BigDecimal("28775.25000000")));
        assertEquals(Instant.ofEpochMilli(1753324800000L), candles.get(0).timestamp());
        assertEquals(0, candles.get(1).open().compareTo(new BigDecimal("28775.25000000")));
    }

    @Test
    void fetchRecentCandles_emptyKlines_throwsNoPriceData() {
        server.expect(requestToUriTemplate(
                        "https://api.binance.com/api/v3/klines?symbol=ZZZZZZZ&interval=1d&limit=2"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThrows(NoPriceDataException.class, () -> client.fetchRecentCandles("ZZZZZZZ", 2));
    }

    @Test
    void fetchRecentCandles_invalidSymbol_throwsNoPriceData() {
        server.expect(requestToUriTemplate(
                        "https://api.binance.com/api/v3/klines?symbol=NOTREAL&interval=1d&limit=2"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":-1121,\"msg\":\"Invalid symbol.\"}"));

        assertThrows(NoPriceDataException.class, () -> client.fetchRecentCandles("NOTREAL", 2));
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/marketdata/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
