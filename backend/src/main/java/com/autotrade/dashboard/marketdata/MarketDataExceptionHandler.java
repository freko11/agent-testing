package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.ticker.TickerAssetTypeConflictException;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central mapping from ticker/market-data domain exceptions to structured
 * JSON error bodies. First {@code @RestControllerAdvice} in the app; future
 * stories with unrelated error domains should decide then whether to add
 * their own or extend this one.
 */
@RestControllerAdvice
public class MarketDataExceptionHandler {

    @ExceptionHandler(TickerNotRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleTickerNotRegistered(TickerNotRegisteredException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("TICKER_NOT_REGISTERED", e.getMessage()));
    }

    @ExceptionHandler(TickerAssetTypeConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAssetTypeConflict(TickerAssetTypeConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ASSET_TYPE_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(NoPriceDataException.class)
    public ResponseEntity<ApiErrorResponse> handleNoPriceData(NoPriceDataException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NO_PRICE_DATA", e.getMessage()));
    }

    @ExceptionHandler(InvalidPriceHistoryRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidPriceHistoryRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("INVALID_REQUEST", message));
    }

    @ExceptionHandler(MarketDataRateLimitedException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimited(MarketDataRateLimitedException e) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (e.retryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()));
        }
        return builder.body(ApiErrorResponse.of("MARKET_DATA_RATE_LIMITED", e.getMessage(), e.source().name()));
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnavailable(MarketDataUnavailableException e) {
        String source = e.source() != null ? e.source().name() : null;
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of("MARKET_DATA_UNAVAILABLE", e.getMessage(), source));
    }
}
