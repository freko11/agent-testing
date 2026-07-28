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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Proves the JSON->Candle mapping and auth-header wiring for E2-F1-S1's
 * Alpaca client, without any live HTTP call or Spring context.
 */
class AlpacaMarketDataClientTest {

    private MockRestServiceServer server;
    private AlpacaMarketDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://data.alpaca.markets");
        server = MockRestServiceServer.bindTo(builder).build();
        AlpacaMarketDataProperties properties = new AlpacaMarketDataProperties(
                "https://data.alpaca.markets", "test-key", "test-secret");
        client = new AlpacaMarketDataClient(builder.build(), properties);
    }

    @Test
    void fetchRecentCandles_parsesBarsAndSendsAuthHeaders() throws IOException {
        server.expect(requestToUriTemplate(
                        "https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=1Day&limit=2&feed=iex"))
                .andExpect(header("APCA-API-KEY-ID", "test-key"))
                .andExpect(header("APCA-API-SECRET-KEY", "test-secret"))
                .andRespond(withSuccess(fixture("alpaca-bars-sample.json"), MediaType.APPLICATION_JSON));

        List<Candle> candles = client.fetchRecentCandles("AAPL", 2);

        assertEquals(2, candles.size());
        assertEquals(0, candles.get(0).close().compareTo(new BigDecimal("150.25")));
        assertEquals(0, candles.get(1).open().compareTo(new BigDecimal("150.30")));
    }

    @Test
    void fetchRecentCandles_missingCredentials_throwsUnavailableWithoutCallingHttp() {
        AlpacaMarketDataProperties noCreds = new AlpacaMarketDataProperties("https://data.alpaca.markets", "", "");
        AlpacaMarketDataClient uncredentialed = new AlpacaMarketDataClient(
                RestClient.builder().baseUrl("https://data.alpaca.markets").build(), noCreds);

        assertThrows(MarketDataUnavailableException.class, () -> uncredentialed.fetchRecentCandles("AAPL", 2));
        server.verify();
    }

    @Test
    void fetchRecentCandles_emptyBars_throwsNoPriceData() {
        server.expect(requestToUriTemplate(
                        "https://data.alpaca.markets/v2/stocks/bars?symbols=ZZZZ&timeframe=1Day&limit=2&feed=iex"))
                .andRespond(withSuccess("{\"bars\":{}}", MediaType.APPLICATION_JSON));

        assertThrows(NoPriceDataException.class, () -> client.fetchRecentCandles("ZZZZ", 2));
    }

    @Test
    void fetchRecentCandles_serverErrorRetriedOnceThenFails_throwsUnavailable() {
        server.expect(requestToUriTemplate(
                        "https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=1Day&limit=2&feed=iex"))
                .andRespond(withServerError());
        server.expect(requestToUriTemplate(
                        "https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=1Day&limit=2&feed=iex"))
                .andRespond(withServerError());

        assertThrows(MarketDataUnavailableException.class, () -> client.fetchRecentCandles("AAPL", 2));
        server.verify();
    }

    @Test
    void fetchRecentCandles_rateLimited_throwsRateLimitedWithRetryAfter() {
        server.expect(requestToUriTemplate(
                        "https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=1Day&limit=2&feed=iex"))
                .andRespond(withServerError());
        server.expect(requestToUriTemplate(
                        "https://data.alpaca.markets/v2/stocks/bars?symbols=AAPL&timeframe=1Day&limit=2&feed=iex"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "30"));

        MarketDataRateLimitedException ex = assertThrows(MarketDataRateLimitedException.class,
                () -> client.fetchRecentCandles("AAPL", 2));
        assertEquals(30L, ex.retryAfterSeconds());
    }

    private String fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/marketdata/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
