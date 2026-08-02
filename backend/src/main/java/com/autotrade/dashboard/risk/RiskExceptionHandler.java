package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.marketdata.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Separate advice class from {@code MarketDataExceptionHandler}/{@code OrderExceptionHandler}/{@code
 * TradingModeExceptionHandler}/{@code NotificationExceptionHandler}, per {@code MarketDataExceptionHandler}'s own
 * documented invitation for an unrelated error domain to decide for itself.
 */
@RestControllerAdvice
public class RiskExceptionHandler {

    @ExceptionHandler(RiskLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRiskLimitExceeded(RiskLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("RISK_LIMIT_EXCEEDED", e.getMessage(), null));
    }

    @ExceptionHandler(KillSwitchEngagedException.class)
    public ResponseEntity<ApiErrorResponse> handleKillSwitchEngaged(KillSwitchEngagedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("KILL_SWITCH_ENGAGED", e.getMessage(), null));
    }
}
