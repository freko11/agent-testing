package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;

import java.time.Instant;

/**
 * The current global trading mode plus when it was last explicitly changed (null if never), and progress
 * toward the two independent gates that unlock LIVE mode: the paper-trade threshold (E6-F1-S2, {@code
 * paperTradeThresholdMet}) and the one-time risk-consent acknowledgment (E6-F1-S3, {@code riskConsentGiven}/
 * {@code riskConsentGivenAt}). {@code liveModeUnlocked} is true only when both gates pass — it's what
 * actually predicts whether {@code switchTo(LIVE)} would succeed right now, while {@code
 * paperTradeThresholdMet} lets the frontend distinguish which gate is still blocking so it can show the
 * right disabled-button reason or consent prompt instead of only reacting to a failed switch attempt.
 */
public record TradingModeResponse(
        TradingMode mode,
        Instant changedAt,
        long successfulPaperTrades,
        int paperTradeThreshold,
        boolean paperTradeThresholdMet,
        boolean riskConsentGiven,
        Instant riskConsentGivenAt,
        boolean liveModeUnlocked) {
}
