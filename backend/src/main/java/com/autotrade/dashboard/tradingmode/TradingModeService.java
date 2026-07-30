package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;
import org.springframework.stereotype.Service;

/**
 * The single global paper/live switch (E6-F1-S1) — read by {@code
 * OrderService} instead of a hardcoded {@code TradingMode.PAPER}. Every
 * mode change is a fresh append-only {@link TradingModeEvent} row; the
 * "current" mode is always the latest one, defaulting to {@code PAPER} on a
 * fresh install with no history yet. Reads are never cached — a fresh DB
 * read on every call, correctness over performance for money-moving code.
 */
@Service
public class TradingModeService {

    private final TradingModeEventRepository repository;

    public TradingModeService(TradingModeEventRepository repository) {
        this.repository = repository;
    }

    public TradingMode current() {
        return currentState().mode();
    }

    public TradingModeResponse currentState() {
        return repository.findTopByOrderByIdDesc()
                .map(event -> new TradingModeResponse(event.getMode(), event.getChangedAt()))
                .orElse(new TradingModeResponse(TradingMode.PAPER, null));
    }

    /**
     * Switching to {@code LIVE} always throws {@link LiveModeNotYetAvailableException} — see that class's Javadoc.
     * Switching to {@code PAPER} always succeeds. Switching to the mode already active is an idempotent no-op (no
     * new history row), matching this codebase's existing idempotent-write conventions ({@code
     * WatchlistService.add}, notification mark-read).
     */
    public TradingModeResponse switchTo(TradingMode requested) {
        if (requested == TradingMode.LIVE) {
            throw new LiveModeNotYetAvailableException();
        }
        if (current() == requested) {
            return currentState();
        }
        TradingModeEvent saved = repository.save(new TradingModeEvent(requested));
        return new TradingModeResponse(saved.getMode(), saved.getChangedAt());
    }
}
