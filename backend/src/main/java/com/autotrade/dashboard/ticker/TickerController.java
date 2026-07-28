package com.autotrade.dashboard.ticker;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Explicit ticker registration — the prerequisite step before GET /api/tickers/{symbol}/price-history works. */
@RestController
@RequestMapping("/api/tickers")
public class TickerController {

    private final TickerService tickerService;

    public TickerController(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    public record RegisterTickerRequest(
            @NotBlank @Size(max = 20) String symbol,
            @NotNull AssetType assetType,
            String exchange) {
    }

    public record TickerResponse(Long id, String symbol, AssetType assetType, String exchange, Instant createdAt) {
        static TickerResponse from(Ticker ticker) {
            return new TickerResponse(ticker.getId(), ticker.getSymbol(), ticker.getAssetType(),
                    ticker.getExchange(), ticker.getCreatedAt());
        }
    }

    @PostMapping
    public ResponseEntity<TickerResponse> register(@Valid @RequestBody RegisterTickerRequest request) {
        boolean existedBefore = tickerService.findRegistered(request.symbol()).isPresent();
        Ticker ticker = tickerService.resolveOrRegister(request.symbol(), request.assetType(), request.exchange());
        HttpStatus status = existedBefore ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(TickerResponse.from(ticker));
    }
}
