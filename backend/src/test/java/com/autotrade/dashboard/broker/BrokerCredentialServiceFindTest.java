package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves {@link BrokerCredentialService#find} — the sole lookup path
 * {@code brokeradapter.AlpacaTradingAdapter} and {@link
 * AlpacaTradingCredentialBootstrap} (E4-F2-S1) both use — against a real
 * repository, including the case no credential has been stored yet.
 */
@SpringBootTest
@Transactional
class BrokerCredentialServiceFindTest {

    @Autowired
    private BrokerCredentialService credentialService;

    @Test
    void find_noCredentialStored_returnsEmpty() {
        assertTrue(credentialService.find(Broker.ALPACA, TradingMode.LIVE).isEmpty());
    }

    @Test
    void find_credentialStored_returnsIt() {
        BrokerCredential stored = credentialService.store(
                Broker.ALPACA, TradingMode.PAPER, "find-test-key", "find-test-secret");

        Optional<BrokerCredential> found = credentialService.find(Broker.ALPACA, TradingMode.PAPER);

        assertTrue(found.isPresent());
        assertEquals(stored.getId(), found.get().getId());
    }
}
