package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.marketdata.ApiErrorResponse;
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

    @ExceptionHandler(LiveModeNotYetAvailableException.class)
    public ResponseEntity<ApiErrorResponse> handleLiveModeNotYetAvailable(LiveModeNotYetAvailableException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse("LIVE_MODE_NOT_YET_AVAILABLE", e.getMessage(), null));
    }
}
