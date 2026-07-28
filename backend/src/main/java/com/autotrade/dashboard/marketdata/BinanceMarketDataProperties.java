package com.autotrade.dashboard.marketdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "marketdata.binance")
public record BinanceMarketDataProperties(String baseUrl) {
}
