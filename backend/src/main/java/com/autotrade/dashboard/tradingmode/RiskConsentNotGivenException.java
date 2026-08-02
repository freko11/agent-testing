package com.autotrade.dashboard.tradingmode;

/**
 * Thrown by {@link TradingModeService#switchTo} when a switch to {@code
 * LIVE} is requested before the one-time risk-consent acknowledgment
 * (E6-F1-S3) has been given. Independent of {@link
 * PaperTradeThresholdNotMetException} (E6-F1-S2) — both gates must pass.
 */
public class RiskConsentNotGivenException extends RuntimeException {

    public RiskConsentNotGivenException() {
        super("Live mode requires an explicit risk-consent acknowledgment before switching.");
    }
}
