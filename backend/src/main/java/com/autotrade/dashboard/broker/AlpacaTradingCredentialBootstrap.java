package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds Alpaca's paper-trading credentials into {@link BrokerCredentialService}
 * from {@code ALPACA_TRADING_API_KEY}/{@code ALPACA_TRADING_API_SECRET} on
 * startup, so {@code brokeradapter.AlpacaTradingAdapter} (E4-F2-S1) always
 * reads through the encrypted, rotation-eligible credential store rather
 * than plain config — the same posture E1-F3-S1 built for exactly this, but
 * with no "connect my account" UI yet to drive it. These are deliberately
 * separate env vars from {@code ALPACA_API_KEY}/{@code ALPACA_API_SECRET},
 * which stay scoped to E2-F1-S1's read-only market-data client.
 *
 * <p>Scoped to {@link TradingMode#PAPER} only — no {@code LIVE} seeding.
 * Idempotent by design, not by accident: if a credential already exists for
 * (ALPACA, PAPER) it is left untouched, since overwriting it on every
 * restart would fight the audited rotation flow ({@link
 * BrokerCredentialService#rotateAll()}) that owns updating it from here on.
 */
@Component
public class AlpacaTradingCredentialBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlpacaTradingCredentialBootstrap.class);

    private final BrokerCredentialService credentialService;
    private final String apiKey;
    private final String apiSecret;

    public AlpacaTradingCredentialBootstrap(BrokerCredentialService credentialService,
                                             @Value("${ALPACA_TRADING_API_KEY:}") String apiKey,
                                             @Value("${ALPACA_TRADING_API_SECRET:}") String apiSecret) {
        this.credentialService = credentialService;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            log.warn("ALPACA_TRADING_API_KEY/ALPACA_TRADING_API_SECRET are not set; the Alpaca paper "
                    + "trading adapter will fail closed until a credential is stored for (ALPACA, PAPER).");
            return;
        }

        if (credentialService.find(Broker.ALPACA, TradingMode.PAPER).isPresent()) {
            log.info("Alpaca paper trading credential already stored for (ALPACA, PAPER); leaving it "
                    + "as-is (use the key-rotation flow to change it, not this bootstrap).");
            return;
        }

        credentialService.store(Broker.ALPACA, TradingMode.PAPER, apiKey, apiSecret);
        log.info("Seeded an Alpaca paper trading credential for (ALPACA, PAPER) from "
                + "ALPACA_TRADING_API_KEY/ALPACA_TRADING_API_SECRET.");
    }
}
