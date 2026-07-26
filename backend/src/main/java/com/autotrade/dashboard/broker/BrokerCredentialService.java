package com.autotrade.dashboard.broker;

import com.autotrade.dashboard.common.TradingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sole entry point for plaintext broker API keys/secrets (E1-F3-S1) — every
 * read or write of a broker credential's plaintext goes through here, never
 * through the entity directly, so encryption and key-version bookkeeping
 * can't be forgotten at a call site.
 */
@Service
public class BrokerCredentialService {

    private static final Logger log = LoggerFactory.getLogger(BrokerCredentialService.class);

    private final BrokerCredentialRepository repository;
    private final CredentialEncryptionService encryptionService;

    public BrokerCredentialService(BrokerCredentialRepository repository,
                                    CredentialEncryptionService encryptionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
    }

    /** Result of decrypting a stored credential. Never log or serialize this. */
    public record DecryptedCredential(String apiKey, String apiSecret) {
    }

    @Transactional
    public BrokerCredential store(Broker broker, TradingMode environment,
                                   String apiKeyPlaintext, String apiSecretPlaintext) {
        BrokerCredential credential = new BrokerCredential(
                broker, environment,
                encryptionService.encrypt(apiKeyPlaintext),
                encryptionService.encrypt(apiSecretPlaintext));
        credential.setEncryptionKeyVersion(encryptionService.activeKeyId());
        return repository.save(credential);
    }

    @Transactional(readOnly = true)
    public DecryptedCredential readDecrypted(BrokerCredential credential) {
        String keyId = credential.getEncryptionKeyVersion();
        return new DecryptedCredential(
                encryptionService.decrypt(keyId, credential.getApiKeyCiphertext()),
                encryptionService.decrypt(keyId, credential.getApiSecretCiphertext()));
    }

    /**
     * Re-encrypts every row not already on the active key with the active key.
     * Idempotent — safe to run repeatedly during a rotation window while both
     * the old and new keys are still present in the environment. See
     * docs/runbooks/credential-key-rotation.md for the full procedure.
     */
    @Transactional
    public int rotateAll() {
        String activeKeyId = encryptionService.activeKeyId();
        List<BrokerCredential> stale = repository.findAll().stream()
                .filter(c -> !activeKeyId.equalsIgnoreCase(c.getEncryptionKeyVersion()))
                .toList();

        for (BrokerCredential credential : stale) {
            DecryptedCredential plaintext = readDecrypted(credential);
            credential.setApiKeyCiphertext(encryptionService.encrypt(plaintext.apiKey()));
            credential.setApiSecretCiphertext(encryptionService.encrypt(plaintext.apiSecret()));
            credential.setEncryptionKeyVersion(activeKeyId);
        }
        repository.saveAll(stale);

        log.info("Credential key rotation: re-encrypted {} row(s) to key id '{}'", stale.size(), activeKeyId);
        return stale.size();
    }
}
