package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.marketdata.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Separate advice class from {@code MarketDataExceptionHandler}/{@code OrderExceptionHandler}/{@code
 * NotificationExceptionHandler}, per {@code MarketDataExceptionHandler}'s own documented invitation for an
 * unrelated error domain to decide for itself.
 */
@RestControllerAdvice
public class TradingModeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TradingModeExceptionHandler.class);

    @ExceptionHandler(PaperTradeThresholdNotMetException.class)
    public ResponseEntity<ApiErrorResponse> handlePaperTradeThresholdNotMet(PaperTradeThresholdNotMetException e) {
        log.info("PAPER_TRADE_THRESHOLD_NOT_MET: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("PAPER_TRADE_THRESHOLD_NOT_MET", e.getMessage(), null));
    }

    @ExceptionHandler(RiskConsentNotGivenException.class)
    public ResponseEntity<ApiErrorResponse> handleRiskConsentNotGiven(RiskConsentNotGivenException e) {
        log.info("RISK_CONSENT_REQUIRED: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("RISK_CONSENT_REQUIRED", e.getMessage(), null));
    }
}
