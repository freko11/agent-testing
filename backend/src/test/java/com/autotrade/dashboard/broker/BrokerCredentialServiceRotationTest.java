package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves E1-F3-S1's rotation flow end-to-end against a real repository: a
 * row written under one key id is re-encrypted under a new active key id by
 * {@link BrokerCredentialService#rotateAll()}, and remains readable
 * throughout — this is what docs/runbooks/credential-key-rotation.md
 * describes operationally.
 */
@SpringBootTest
@Transactional
class BrokerCredentialServiceRotationTest {

    @Autowired
    private BrokerCredentialRepository repository;

    @Test
    void rotateAll_reencryptsStaleRowsAndLeavesThemReadable() {
        CredentialEncryptionService v1Only = new CredentialEncryptionService(
                Map.of("CREDENTIAL_ENC_KEY_V1", "test-key-one"));
        BrokerCredentialService beforeRotation = new BrokerCredentialService(repository, v1Only);

        BrokerCredential credential = beforeRotation.store(
                Broker.ALPACA, TradingMode.PAPER, "rotate-key", "rotate-secret");
        repository.flush();
        assertEquals("v1", credential.getEncryptionKeyVersion());

        CredentialEncryptionService v1AndV2Active = new CredentialEncryptionService(Map.of(
                "CREDENTIAL_ENC_KEY_V1", "test-key-one",
                "CREDENTIAL_ENC_KEY_V2", "test-key-two",
                "CREDENTIAL_ENC_ACTIVE_KEY_ID", "v2"));
        BrokerCredentialService duringRotation = new BrokerCredentialService(repository, v1AndV2Active);

        int rotated = duringRotation.rotateAll();
        repository.flush();
        assertEquals(1, rotated);

        BrokerCredential afterRotation = repository.findById(credential.getId()).orElseThrow();
        assertEquals("v2", afterRotation.getEncryptionKeyVersion());
        BrokerCredentialService.DecryptedCredential decrypted = duringRotation.readDecrypted(afterRotation);
        assertEquals("rotate-key", decrypted.apiKey());
        assertEquals("rotate-secret", decrypted.apiSecret());

        // Idempotent — running it again finds nothing left to rotate.
        assertEquals(0, duringRotation.rotateAll());
    }
}
