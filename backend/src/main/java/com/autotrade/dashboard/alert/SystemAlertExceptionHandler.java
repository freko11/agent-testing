package com.autotrade.dashboard.alert;

import com.autotrade.dashboard.marketdata.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Separate advice class from {@code MarketDataExceptionHandler}/{@code
 * NotificationExceptionHandler}, per {@code MarketDataExceptionHandler}'s own documented
 * invitation for an unrelated error domain to decide for itself.
 */
@RestControllerAdvice
public class SystemAlertExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SystemAlertExceptionHandler.class);

    @ExceptionHandler(InvalidSystemAlertRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidSystemAlertRequestException e) {
        log.info("INVALID_REQUEST (system-alert): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", e.getMessage(), null));
    }
}
