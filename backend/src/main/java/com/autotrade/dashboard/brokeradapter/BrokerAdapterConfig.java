package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Map;

/**
 * Wires the Alpaca trading adapter (E4-F2-S1). Nothing currently consumes a
 * {@link BrokerAdapter} bean — E5's order-submission flow doesn't exist yet —
 * same "bean nothing wires up yet" situation E4-F1-S1's interface addition
 * already accepted, needed now so E4-F2-S2 (place a market order) has one to
 * inject immediately.
 */
@Configuration
@EnableConfigurationProperties(AlpacaTradingProperties.class)
public class BrokerAdapterConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    @Bean
    public Map<TradingMode, RestClient> alpacaTradingRestClients(AlpacaTradingProperties properties) {
        return Map.of(
                TradingMode.PAPER, RestClient.builder().baseUrl(properties.paperBaseUrl()).requestFactory(requestFactory()).build(),
                TradingMode.LIVE, RestClient.builder().baseUrl(properties.liveBaseUrl()).requestFactory(requestFactory()).build());
    }

    /**
     * The actual exposed {@link BrokerAdapter} bean — {@link AlpacaTradingAdapter}
     * itself is a plain, non-{@code @Component} class, wrapped here in {@link
     * RetryingBrokerAdapter} so every call gets uniform retry/backoff/outage
     * handling (E4-F1-S2/S3), the same "wrap your adapter" template {@code
     * RetryingMockBrokerAdapterContractTest} already documents.
     */
    @Bean
    public BrokerAdapter alpacaBrokerAdapter(Map<TradingMode, RestClient> alpacaTradingRestClients,
                                              BrokerCredentialService credentialService,
                                              Clock clock) {
        AlpacaTradingAdapter delegate = new AlpacaTradingAdapter(alpacaTradingRestClients, credentialService, clock);
        return new RetryingBrokerAdapter(delegate);
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        return factory;
    }
}
