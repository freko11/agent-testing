package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Map;

/**
 * Wires the Binance Futures Testnet trading adapter (E4-F3-S1) — a sibling
 * to {@link BrokerAdapterConfig} (Alpaca), kept separate rather than folded
 * together since each config class is scoped to one broker's base
 * URLs/properties. Nothing currently consumes this {@link BrokerAdapter}
 * bean either — same "bean nothing wires up yet" situation Alpaca's own
 * config already accepted.
 */
@Configuration
@EnableConfigurationProperties(BinanceFuturesTradingProperties.class)
public class BinanceFuturesAdapterConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    @Bean
    public Map<TradingMode, RestClient> binanceFuturesTradingRestClients(BinanceFuturesTradingProperties properties) {
        return Map.of(
                TradingMode.PAPER, RestClient.builder().baseUrl(properties.paperBaseUrl()).requestFactory(requestFactory()).build(),
                TradingMode.LIVE, RestClient.builder().baseUrl(properties.liveBaseUrl()).requestFactory(requestFactory()).build());
    }

    /**
     * The actual exposed {@link BrokerAdapter} bean — {@link
     * BinanceFuturesTradingAdapter} itself is a plain, non-{@code @Component}
     * class, wrapped here in {@link RetryingBrokerAdapter} for uniform
     * retry/backoff/outage handling, same template as Alpaca's config.
     */
    @Bean
    public BrokerAdapter binanceBrokerAdapter(
            @Qualifier("binanceFuturesTradingRestClients") Map<TradingMode, RestClient> binanceFuturesTradingRestClients,
            BrokerCredentialService credentialService,
            Clock clock) {
        BinanceFuturesTradingAdapter delegate =
                new BinanceFuturesTradingAdapter(binanceFuturesTradingRestClients, credentialService, clock);
        return new RetryingBrokerAdapter(delegate);
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        return factory;
    }
}
