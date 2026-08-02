package com.autotrade.dashboard.broker;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for E1-F3-S1's key-rotation support: multiple keys can be live
 * at once, new writes use the active key, and old ciphertext stays
 * decryptable via its own recorded key id.
 */
class CredentialEncryptionServiceTest {

    @Test
    void noKeysConfigured_fallsBackToDevOnlyKey() {
        CredentialEncryptionService service = new CredentialEncryptionService(Map.of());

        String ciphertext = service.encrypt("secret-value");
        assertEquals("secret-value", service.decrypt(service.activeKeyId(), ciphertext));
    }

    @Test
    void noKeysConfigured_underPaperProfile_failsFastInsteadOfUsingDevKey() {
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService(Map.of(), "paper"));
    }

    @Test
    void noKeysConfigured_underProdProfile_failsFastInsteadOfUsingDevKey() {
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService(Map.of(), "prod"));
    }

    @Test
    void noKeysConfigured_underLocalProfile_fallsBackToDevOnlyKey() {
        CredentialEncryptionService service = new CredentialEncryptionService(Map.of(), "local");

        String ciphertext = service.encrypt("secret-value");
        assertEquals("secret-value", service.decrypt(service.activeKeyId(), ciphertext));
    }

    @Test
    void singleKeyConfigured_usedAsActiveWithoutExplicitSelection() {
        CredentialEncryptionService service = new CredentialEncryptionService(
                Map.of("CREDENTIAL_ENC_KEY_V1", "test-key-one"));

        assertEquals("v1", service.activeKeyId());
        String ciphertext = service.encrypt("secret-value");
        assertEquals("secret-value", service.decrypt("v1", ciphertext));
    }

    @Test
    void multipleKeysWithoutActiveIdSelected_failsFast() {
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService(Map.of(
                "CREDENTIAL_ENC_KEY_V1", "test-key-one",
                "CREDENTIAL_ENC_KEY_V2", "test-key-two")));
    }

    @Test
    void activeKeyIdWithNoMatchingEnvVar_failsFast() {
        assertThrows(IllegalStateException.class, () -> new CredentialEncryptionService(Map.of(
                "CREDENTIAL_ENC_KEY_V1", "test-key-one",
                "CREDENTIAL_ENC_ACTIVE_KEY_ID", "v9")));
    }

    @Test
    void rotationScenario_oldCiphertextStillDecryptsWhileActiveKeyMovesToNewId() {
        CredentialEncryptionService v1Only = new CredentialEncryptionService(
                Map.of("CREDENTIAL_ENC_KEY_V1", "test-key-one"));
        String ciphertextUnderV1 = v1Only.encrypt("secret-value");

        CredentialEncryptionService rotated = new CredentialEncryptionService(Map.of(
                "CREDENTIAL_ENC_KEY_V1", "test-key-one",
                "CREDENTIAL_ENC_KEY_V2", "test-key-two",
                "CREDENTIAL_ENC_ACTIVE_KEY_ID", "v2"));

        assertEquals("v2", rotated.activeKeyId());
        // Old ciphertext, encrypted under v1, is still decryptable by its own key id.
        assertEquals("secret-value", rotated.decrypt("v1", ciphertextUnderV1));
        // New writes go out under the active key.
        String ciphertextUnderV2 = rotated.encrypt("secret-value");
        assertNotEquals(ciphertextUnderV1, ciphertextUnderV2);
        assertEquals("secret-value", rotated.decrypt("v2", ciphertextUnderV2));
    }

    @Test
    void decryptWithUnknownKeyId_failsFastWithoutLeakingPlaintext() {
        CredentialEncryptionService service = new CredentialEncryptionService(
                Map.of("CREDENTIAL_ENC_KEY_V1", "test-key-one"));
        String ciphertext = service.encrypt("secret-value");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.decrypt("v9", ciphertext));
        assertEquals(
                "No encryption key registered for id 'v9' — it may have been removed "
                        + "from the environment before rotation finished for all rows.",
                ex.getMessage());
    }
}
