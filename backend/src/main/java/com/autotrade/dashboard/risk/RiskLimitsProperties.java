package com.autotrade.dashboard.risk;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Configured hard caps enforced by {@link RiskLimitService} (E6-F2-S1's
 * per-order caps, E6-F2-S3's portfolio-level aggregate exposure cap) —
 * config-only, matching {@code trading-mode.paper-trade-threshold}'s
 * precedent for a personal, single-operator tool's safety knobs: set
 * deliberately, changed rarely, via env var + restart rather than a DB-backed
 * table. Stock leverage isn't configurable here since {@code
 * OrderService.validate} already hard-locks it to 1x.
 */
@ConfigurationProperties(prefix = "risk-limits")
public record RiskLimitsProperties(
        BigDecimal stockMaxPositionSizeUsd,
        BigDecimal cryptoMaxLeverage,
        BigDecimal cryptoMaxPositionSizeUsd,
        BigDecimal maxAggregateExposureUsd) {
}
