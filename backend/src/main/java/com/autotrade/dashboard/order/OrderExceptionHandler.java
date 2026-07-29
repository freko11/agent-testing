package com.autotrade.dashboard.order;

import com.autotrade.dashboard.marketdata.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps trade-submission domain exceptions to structured JSON error bodies —
 * a separate advice class from {@code MarketDataExceptionHandler}, per that
 * class's own documented invitation for an unrelated error domain to decide
 * for itself, since order submission is a genuinely different (money-moving)
 * domain from market-data reads. Does not redeclare {@code
 * MethodArgumentNotValidException}, since that's already handled globally by
 * {@code MarketDataExceptionHandler}'s own bean.
 */
@RestControllerAdvice
public class OrderExceptionHandler {

    @ExceptionHandler(SignalNotActionableException.class)
    public ResponseEntity<ApiErrorResponse> handleSignalNotActionable(SignalNotActionableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse("SIGNAL_NOT_ACTIONABLE", e.getMessage(), null));
    }

    @ExceptionHandler(InvalidTradeRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTradeRequest(InvalidTradeRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse("INVALID_REQUEST", e.getMessage(), null));
    }

    @ExceptionHandler(BrokerCredentialNotConfiguredException.class)
    public ResponseEntity<ApiErrorResponse> handleBrokerCredentialNotConfigured(BrokerCredentialNotConfiguredException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponse("BROKER_CREDENTIAL_NOT_CONFIGURED", e.getMessage(), null));
    }
}
