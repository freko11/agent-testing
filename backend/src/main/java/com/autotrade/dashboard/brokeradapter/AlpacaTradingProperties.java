package com.autotrade.dashboard.brokeradapter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs only — deliberately no {@code apiKey}/{@code apiSecret} fields
 * here, unlike {@code marketdata.AlpacaMarketDataProperties}. Trading
 * credentials come from {@code broker.BrokerCredentialService} (encrypted,
 * rotation-eligible), not plain config; see {@code
 * broker.AlpacaTradingCredentialBootstrap} for how they get there.
 */
@ConfigurationProperties(prefix = "broker.alpaca")
public record AlpacaTradingProperties(String paperBaseUrl, String liveBaseUrl) {
}
