package com.autotrade.dashboard.marketdata;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({AlpacaMarketDataProperties.class, BinanceMarketDataProperties.class})
public class MarketDataClientConfig {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    // First app-wide Clock consumer (MarketHoursService); move to a dedicated config
    // if a second, unrelated consumer shows up later.
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient alpacaRestClient(AlpacaMarketDataProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory())
                .build();
    }

    @Bean
    public RestClient binanceRestClient(BinanceMarketDataProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory())
                .build();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        factory.setReadTimeout(READ_TIMEOUT_MILLIS);
        return factory;
    }
}
