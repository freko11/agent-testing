package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.marketdata.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Separate advice class from {@code MarketDataExceptionHandler}/{@code
 * OrderExceptionHandler}, per {@code MarketDataExceptionHandler}'s own
 * documented invitation for an unrelated error domain to decide for itself.
 */
@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(InvalidNotificationRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidNotificationRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", e.getMessage(), null));
    }
}
