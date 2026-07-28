package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.ticker.AssetType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Binance's public klines endpoint — crypto only, no authentication required for read-only market data. */
@Component
public class BinanceMarketDataClient implements MarketDataClient {

    private final RestClient restClient;

    public BinanceMarketDataClient(@Qualifier("binanceRestClient") RestClient restClient) {
        this.restClient = restClient;
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
    public List<Candle> fetchRecentCandles(String symbol, int limit) {
        try {
            List<List<Object>> klines = RetryHelper.withOneRetry(() -> restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/v3/klines")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "1d")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<List<Object>>>() {
                    }));

            if (klines == null || klines.isEmpty()) {
                throw new NoPriceDataException("Binance returned no klines for symbol '" + symbol + "'");
            }

            return klines.stream().map(BinanceMarketDataClient::toCandle).toList();
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new MarketDataRateLimitedException(Broker.BINANCE, retryAfterSeconds(e));
        } catch (HttpClientErrorException.BadRequest e) {
            // Binance returns 400 {"code":-1121,"msg":"Invalid symbol."} for unknown symbols.
            throw new NoPriceDataException("Binance has no data for symbol '" + symbol + "': " + e.getMessage());
        } catch (RestClientException e) {
            throw new MarketDataUnavailableException(Broker.BINANCE, "Binance market data request failed: " + e.getMessage());
        }
    }

    private static Candle toCandle(List<Object> kline) {
        long openTimeMillis = ((Number) kline.get(0)).longValue();
        BigDecimal open = new BigDecimal((String) kline.get(1));
        BigDecimal high = new BigDecimal((String) kline.get(2));
        BigDecimal low = new BigDecimal((String) kline.get(3));
        BigDecimal close = new BigDecimal((String) kline.get(4));
        BigDecimal volume = new BigDecimal((String) kline.get(5));
        return new Candle(Instant.ofEpochMilli(openTimeMillis), open, high, low, close, volume);
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
}
