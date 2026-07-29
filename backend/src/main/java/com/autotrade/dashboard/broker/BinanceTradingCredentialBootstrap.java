package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds Binance Futures Testnet credentials into {@link BrokerCredentialService}
 * from {@code BINANCE_TRADING_API_KEY}/{@code BINANCE_TRADING_API_SECRET} on
 * startup, so {@code brokeradapter.BinanceFuturesTradingAdapter} (E4-F3-S1)
 * always reads through the encrypted, rotation-eligible credential store
 * rather than plain config — the same posture {@link
 * AlpacaTradingCredentialBootstrap} already established for Alpaca. These
 * are a Futures Testnet-only key pair, generated at
 * {@code testnet.binancefuture.com} — a separate testnet-only account, not a
 * real Binance.com login, and not the same key {@code
 * marketdata.BinanceMarketDataClient} would use (that client needs no
 * credentials at all, since it only reads Binance's public klines endpoint).
 *
 * <p>Scoped to {@link TradingMode#PAPER} only — no {@code LIVE} seeding.
 * Idempotent by design: if a credential already exists for
 * (BINANCE, PAPER) it is left untouched, since overwriting it on every
 * restart would fight the audited rotation flow ({@link
 * BrokerCredentialService#rotateAll()}) that owns updating it from here on.
 */
@Component
public class BinanceTradingCredentialBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BinanceTradingCredentialBootstrap.class);

    private final BrokerCredentialService credentialService;
    private final String apiKey;
    private final String apiSecret;

    public BinanceTradingCredentialBootstrap(BrokerCredentialService credentialService,
                                              @Value("${BINANCE_TRADING_API_KEY:}") String apiKey,
                                              @Value("${BINANCE_TRADING_API_SECRET:}") String apiSecret) {
        this.credentialService = credentialService;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            log.warn("BINANCE_TRADING_API_KEY/BINANCE_TRADING_API_SECRET are not set; the Binance Futures "
                    + "Testnet trading adapter will fail closed until a credential is stored for (BINANCE, PAPER).");
            return;
        }

        if (credentialService.find(Broker.BINANCE, TradingMode.PAPER).isPresent()) {
            log.info("Binance Futures Testnet trading credential already stored for (BINANCE, PAPER); leaving it "
                    + "as-is (use the key-rotation flow to change it, not this bootstrap).");
            return;
        }

        credentialService.store(Broker.BINANCE, TradingMode.PAPER, apiKey, apiSecret);
        log.info("Seeded a Binance Futures Testnet trading credential for (BINANCE, PAPER) from "
                + "BINANCE_TRADING_API_KEY/BINANCE_TRADING_API_SECRET.");
    }
}
