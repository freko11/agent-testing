package com.autotrade.dashboard.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for E7-F2-S1's security-review fix: the dev-only fallback
 * password must never be used under the {@code paper}/{@code prod} profiles.
 * Exercises {@link SecurityConfig#resolvePasswordHash} directly — no Spring
 * context needed, mirroring {@code CredentialEncryptionServiceTest}'s plain
 * constructor-level testing of the analogous encryption-key fallback.
 */
class SecurityConfigPasswordFallbackTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void noPasswordConfigured_underPaperProfile_failsFastInsteadOfUsingDevPassword() {
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.resolvePasswordHash("", "", "paper", passwordEncoder));
    }

    @Test
    void noPasswordConfigured_underProdProfile_failsFastInsteadOfUsingDevPassword() {
        assertThrows(IllegalStateException.class,
                () -> SecurityConfig.resolvePasswordHash("", "", "prod", passwordEncoder));
    }

    @Test
    void noPasswordConfigured_underLocalProfile_fallsBackToDevPassword() {
        String hash = SecurityConfig.resolvePasswordHash("", "", "local", passwordEncoder);

        assertTrue(passwordEncoder.matches(
                "insecure-dev-only-password-do-not-use-in-paper-or-prod", hash));
    }

    @Test
    void explicitPasswordHashConfigured_underPaperProfile_usedAsIs() {
        String hash = SecurityConfig.resolvePasswordHash("", "already-hashed-value", "paper", passwordEncoder);

        assertTrue(hash.equals("already-hashed-value"));
    }
}
