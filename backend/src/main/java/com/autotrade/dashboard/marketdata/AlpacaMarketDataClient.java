package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.ticker.AssetType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Alpaca's historical-bars market data endpoint — stocks only. Auth is required even for read-only bars. */
@Component
public class AlpacaMarketDataClient implements MarketDataClient {

    private final RestClient restClient;
    private final AlpacaMarketDataProperties properties;

    public AlpacaMarketDataClient(@Qualifier("alpacaRestClient") RestClient restClient,
                                   AlpacaMarketDataProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
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
    public List<Candle> fetchRecentCandles(String symbol, int limit) {
        if (isBlank(properties.apiKey()) || isBlank(properties.apiSecret())) {
            throw new MarketDataUnavailableException(Broker.ALPACA,
                    "Alpaca market data credentials are not configured (ALPACA_API_KEY/ALPACA_API_SECRET)");
        }

        try {
            AlpacaBarsResponse response = RetryHelper.withOneRetry(() -> restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/stocks/bars")
                            .queryParam("symbols", symbol)
                            .queryParam("timeframe", "1Day")
                            .queryParam("limit", limit)
                            .queryParam("feed", "iex")
                            .build())
                    .header("APCA-API-KEY-ID", properties.apiKey())
                    .header("APCA-API-SECRET-KEY", properties.apiSecret())
                    .retrieve()
                    .body(AlpacaBarsResponse.class));

            List<AlpacaBar> bars = Optional.ofNullable(response)
                    .map(AlpacaBarsResponse::bars)
                    .map(barsBySymbol -> barsBySymbol.get(symbol))
                    .orElse(List.of());

            if (bars.isEmpty()) {
                throw new NoPriceDataException("Alpaca returned no bars for symbol '" + symbol + "'");
            }

            return bars.stream()
                    .map(bar -> new Candle(bar.t(), bar.o(), bar.h(), bar.l(), bar.c(), bar.v()))
                    .toList();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new MarketDataRateLimitedException(Broker.ALPACA, retryAfterSeconds(e));
        } catch (HttpClientErrorException.NotFound e) {
            throw new NoPriceDataException("Alpaca has no data for symbol '" + symbol + "'");
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException(Broker.ALPACA, "Alpaca market data request failed: " + e.getMessage());
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Long retryAfterSeconds(HttpClientErrorException.TooManyRequests e) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlpacaBarsResponse(Map<String, List<AlpacaBar>> bars) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlpacaBar(
            @JsonProperty("t") Instant t,
            @JsonProperty("o") BigDecimal o,
            @JsonProperty("h") BigDecimal h,
            @JsonProperty("l") BigDecimal l,
            @JsonProperty("c") BigDecimal c,
            @JsonProperty("v") BigDecimal v) {
    }
}
