package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.indicator.InsufficientPriceHistoryException;
import com.autotrade.dashboard.indicator.InvalidIndicatorRequestException;
import com.autotrade.dashboard.ticker.TickerAssetTypeConflictException;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(MarketDataExceptionHandler.class);

    @ExceptionHandler(TickerNotRegisteredException.class)
    public ResponseEntity<ApiErrorResponse> handleTickerNotRegistered(TickerNotRegisteredException e) {
        log.info("TICKER_NOT_REGISTERED: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("TICKER_NOT_REGISTERED", e.getMessage()));
    }

    @ExceptionHandler(TickerAssetTypeConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleAssetTypeConflict(TickerAssetTypeConflictException e) {
        log.info("ASSET_TYPE_CONFLICT: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("ASSET_TYPE_CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(NoPriceDataException.class)
    public ResponseEntity<ApiErrorResponse> handleNoPriceData(NoPriceDataException e) {
        log.info("NO_PRICE_DATA: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NO_PRICE_DATA", e.getMessage()));
    }

    @ExceptionHandler(InvalidPriceHistoryRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidPriceHistoryRequestException e) {
        log.info("INVALID_REQUEST: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("INVALID_REQUEST", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Invalid request");
        log.info("INVALID_REQUEST (validation): {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("INVALID_REQUEST", message));
    }

    @ExceptionHandler(MarketClosedException.class)
    public ResponseEntity<ApiErrorResponse> handleMarketClosed(MarketClosedException e) {
        log.info("MARKET_CLOSED: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of("MARKET_CLOSED", e.getMessage()));
    }

    @ExceptionHandler(MarketDataRateLimitedException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimited(MarketDataRateLimitedException e) {
        log.warn("MARKET_DATA_RATE_LIMITED: source={} retryAfterSeconds={} {}",
                e.source(), e.retryAfterSeconds(), e.getMessage());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);
        if (e.retryAfterSeconds() != null) {
            builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(e.retryAfterSeconds()));
        }
        return builder.body(ApiErrorResponse.of("MARKET_DATA_RATE_LIMITED", e.getMessage(), e.source().name()));
    }

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnavailable(MarketDataUnavailableException e) {
        String source = e.source() != null ? e.source().name() : null;
        log.warn("MARKET_DATA_UNAVAILABLE: source={} {}", source, e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.of("MARKET_DATA_UNAVAILABLE", e.getMessage(), source));
    }

    @ExceptionHandler(InsufficientPriceHistoryException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientPriceHistory(InsufficientPriceHistoryException e) {
        log.info("INSUFFICIENT_PRICE_HISTORY: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiErrorResponse.of("INSUFFICIENT_PRICE_HISTORY", e.getMessage()));
    }

    @ExceptionHandler(InvalidIndicatorRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidIndicatorRequest(InvalidIndicatorRequestException e) {
        log.info("INVALID_REQUEST (indicator): {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("INVALID_REQUEST", e.getMessage()));
    }
}
