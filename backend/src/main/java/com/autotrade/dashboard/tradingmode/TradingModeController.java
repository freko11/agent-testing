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
 * mutating endpoint in this app. The one-time risk-consent acknowledgment (E6-F1-S3) is recorded via its own
 * endpoint rather than a flag on {@link TradingModeChangeRequest}, so consent stays an independently
 * auditable event with its own timestamp, decoupled from any particular switch attempt.
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

    @PostMapping("/risk-consent")
    public TradingModeResponse giveRiskConsent() {
        return tradingModeService.giveRiskConsent();
    }
}
