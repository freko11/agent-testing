package com.autotrade.dashboard.broker;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES/GCM converter for the broker_credentials ciphertext columns.
 *
 * <p>This is the minimal safeguard called for by E1-F2-S1 — full key
 * rotation / keystore-backed encryption is separate future work (F1.3).
 * The symmetric key comes from the {@code CREDENTIAL_ENC_KEY} env var (any
 * string; SHA-256 hashed into a 256-bit AES key). If unset, a fixed
 * dev-only fallback key is used and a warning is logged — acceptable for
 * local/test only, never for paper/prod.
 */
@Converter
public class CredentialCipherConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(CredentialCipherConverter.class);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String DEV_FALLBACK_KEY =
            "insecure-dev-only-fallback-key-do-not-use-in-paper-or-prod";

    private final SecretKeySpec keySpec;

    public CredentialCipherConverter() {
        String rawKey = System.getenv("CREDENTIAL_ENC_KEY");
        if (rawKey == null || rawKey.isBlank()) {
            log.warn("CREDENTIAL_ENC_KEY is not set; using an insecure dev-only fallback key. "
                    + "Set CREDENTIAL_ENC_KEY in paper/prod environments.");
            rawKey = DEV_FALLBACK_KEY;
        }
        this.keySpec = deriveKey(rawKey);
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

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (plaintext == null) {
            return null;
        }
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

    @Override
    public String convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbValue);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            byte[] ciphertext = new byte[combined.length - iv.length];
            System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt broker credential", e);
        }
    }
}
