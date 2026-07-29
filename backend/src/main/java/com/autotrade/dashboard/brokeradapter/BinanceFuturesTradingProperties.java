package com.autotrade.dashboard.brokeradapter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs only — same posture as {@link AlpacaTradingProperties}, no
 * credential fields; trading credentials come from {@code
 * broker.BrokerCredentialService} (see {@code
 * broker.BinanceTradingCredentialBootstrap}). Field names stay {@code
 * paper}/{@code live} for symmetry with Alpaca's properties even though
 * Binance itself calls its non-live environment "Futures Testnet."
 */
@ConfigurationProperties(prefix = "broker.binance")
public record BinanceFuturesTradingProperties(String paperBaseUrl, String liveBaseUrl) {
}
