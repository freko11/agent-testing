package com.autotrade.dashboard.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * AES/GCM encryption for broker_credentials' ciphertext columns, keyed by a
 * rotatable {@code encryption_key_version} id rather than a single fixed key
 * (E1-F3-S1). Supersedes the old {@code CredentialCipherConverter}, which
 * could only ever use one key and had no way to read/write the version
 * column a transparent JPA converter can't see sibling fields.
 *
 * <p>Keyring is built at startup from every {@code CREDENTIAL_ENC_KEY_<ID>}
 * env var (id lowercased, e.g. {@code CREDENTIAL_ENC_KEY_V2} -> {@code v2}).
 * {@code CREDENTIAL_ENC_ACTIVE_KEY_ID} selects which key new writes use.
 * Rotation keeps the old and new keys both present in the environment until
 * {@link BrokerCredentialService#rotateAll()} has re-encrypted every row —
 * see docs/runbooks/credential-key-rotation.md for the operational sequence.
 *
 * <p>If no {@code CREDENTIAL_ENC_KEY_*} vars are set at all, falls back to a
 * single insecure dev-only key (id {@code v1}) with a logged warning —
 * acceptable for local/test only, never for paper/prod.
 */
@Component
public class CredentialEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(CredentialEncryptionService.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String ENV_KEY_PREFIX = "CREDENTIAL_ENC_KEY_";
    private static final String DEV_FALLBACK_KEY_ID = "v1";
    private static final String DEV_FALLBACK_KEY =
            "insecure-dev-only-fallback-key-do-not-use-in-paper-or-prod";

    private final Map<String, SecretKeySpec> keyring = new HashMap<>();
    private final String activeKeyId;

    public CredentialEncryptionService() {
        this(System.getenv());
    }

    /** Package-private: lets tests supply a synthetic environment instead of the real one. */
    CredentialEncryptionService(Map<String, String> env) {
        env.forEach((name, value) -> {
            if (name.startsWith(ENV_KEY_PREFIX) && !value.isBlank()) {
                String keyId = name.substring(ENV_KEY_PREFIX.length()).toLowerCase();
                keyring.put(keyId, deriveKey(value));
            }
        });

        if (keyring.isEmpty()) {
            log.warn("No CREDENTIAL_ENC_KEY_<ID> env vars are set; using an insecure dev-only "
                    + "fallback key (id '{}'). Set CREDENTIAL_ENC_KEY_V1 and CREDENTIAL_ENC_ACTIVE_KEY_ID "
                    + "in paper/prod environments.", DEV_FALLBACK_KEY_ID);
            keyring.put(DEV_FALLBACK_KEY_ID, deriveKey(DEV_FALLBACK_KEY));
        }

        String configuredActiveId = env.get("CREDENTIAL_ENC_ACTIVE_KEY_ID");
        if (configuredActiveId != null && !configuredActiveId.isBlank()) {
            String requestedId = configuredActiveId.toLowerCase();
            if (!keyring.containsKey(requestedId)) {
                throw new IllegalStateException(
                        "CREDENTIAL_ENC_ACTIVE_KEY_ID='" + requestedId
                                + "' has no matching CREDENTIAL_ENC_KEY_" + requestedId.toUpperCase() + " env var");
            }
            this.activeKeyId = requestedId;
        } else if (keyring.size() == 1) {
            this.activeKeyId = keyring.keySet().iterator().next();
        } else {
            throw new IllegalStateException(
                    "Multiple CREDENTIAL_ENC_KEY_<ID> env vars are set but CREDENTIAL_ENC_ACTIVE_KEY_ID "
                            + "is not — set it to name which key new writes should use.");
        }
    }

    private static SecretKeySpec deriveKey(String rawKey) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to derive credential encryption key", e);
        }
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    public String encrypt(String plaintext) {
        return encryptWithKey(activeKeyId, plaintext);
    }

    public String decrypt(String keyId, String ciphertext) {
        SecretKeySpec keySpec = keyring.get(keyId.toLowerCase());
        if (keySpec == null) {
            throw new IllegalStateException(
                    "No encryption key registered for id '" + keyId + "' — it may have been removed "
                            + "from the environment before rotation finished for all rows.");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            byte[] cipherBytes = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(cipherBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt broker credential with key id '" + keyId + "'", e);
        }
    }

    private String encryptWithKey(String keyId, String plaintext) {
        SecretKeySpec keySpec = keyring.get(keyId);
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt broker credential", e);
        }
    }
}
