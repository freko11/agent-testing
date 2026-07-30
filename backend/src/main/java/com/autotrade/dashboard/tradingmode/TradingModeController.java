package com.autotrade.dashboard.tradingmode;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The global paper/live switch (E6-F1-S1) — a standalone top-level resource, mirroring {@code /api/watchlist}/
 * {@code /api/orders}/{@code /api/notifications}'s precedent. Normal session auth only, same as every other
 * mutating endpoint in this app — a stronger re-auth/confirmation step is E6-F1-S3's job, not this one's.
 */
@RestController
@RequestMapping("/api/trading-mode")
public class TradingModeController {

    private final TradingModeService tradingModeService;

    public TradingModeController(TradingModeService tradingModeService) {
        this.tradingModeService = tradingModeService;
    }

    @GetMapping
    public TradingModeResponse current() {
        return tradingModeService.currentState();
    }

    @PostMapping
    public TradingModeResponse switchMode(@Valid @RequestBody TradingModeChangeRequest request) {
        return tradingModeService.switchTo(request.mode());
    }
}
