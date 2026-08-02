package com.autotrade.dashboard.risk;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds {@link RiskLimitsProperties} — a sibling to {@code BrokerAdapterConfig}/{@code BinanceFuturesAdapterConfig}'s own {@code @EnableConfigurationProperties} pattern, kept separate since it scopes one domain's config. */
@Configuration
@EnableConfigurationProperties(RiskLimitsProperties.class)
public class RiskLimitConfig {
}
