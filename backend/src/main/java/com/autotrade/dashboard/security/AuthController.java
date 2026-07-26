package com.autotrade.dashboard.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Actual login/logout submission goes through Spring Security's formLogin
 * filter chain (see SecurityConfig) — this controller only exposes what
 * that filter chain doesn't: a way to prime the CSRF cookie before the first
 * POST, and a way to check current session state on page load/reload.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }
}
