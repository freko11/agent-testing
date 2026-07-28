package com.autotrade.dashboard.marketdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketdata.alpaca")
public record AlpacaMarketDataProperties(String baseUrl, String apiKey, String apiSecret) {
}
