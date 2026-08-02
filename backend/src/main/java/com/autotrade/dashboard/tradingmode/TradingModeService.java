package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.OrderRepository;
import com.autotrade.dashboard.order.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

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
    private final OrderRepository orderRepository;
    private final int paperTradeThreshold;

    public TradingModeService(
            TradingModeEventRepository repository,
            OrderRepository orderRepository,
            @Value("${trading-mode.paper-trade-threshold}") int paperTradeThreshold) {
        this.repository = repository;
        this.orderRepository = orderRepository;
        this.paperTradeThreshold = paperTradeThreshold;
    }

    public TradingMode current() {
        return currentState().mode();
    }

    public TradingModeResponse currentState() {
        long completed = successfulPaperTrades();
        return repository.findTopByOrderByIdDesc()
                .map(event -> toResponse(event.getMode(), event.getChangedAt(), completed))
                .orElse(toResponse(TradingMode.PAPER, null, completed));
    }

    /**
     * Switching to the mode already active is an idempotent no-op (no new history row) — checked before the
     * LIVE gate below, since a no-op {@code LIVE -> LIVE} shouldn't re-validate a threshold that's irrelevant
     * to a state that's already true. Switching to {@code PAPER} always succeeds. Switching to {@code LIVE}
     * requires at least {@link #paperTradeThreshold} successful (filled, paper-mode) orders (E6-F1-S2) — see
     * {@link PaperTradeThresholdNotMetException}. Note this is only one of two planned LIVE gates: E6-F1-S3's
     * risk-consent step is a second, independent check layered on top, not yet implemented.
     */
    public TradingModeResponse switchTo(TradingMode requested) {
        if (current() == requested) {
            return currentState();
        }
        long completed = successfulPaperTrades();
        if (requested == TradingMode.LIVE && completed < paperTradeThreshold) {
            throw new PaperTradeThresholdNotMetException(completed, paperTradeThreshold);
        }
        TradingModeEvent saved = repository.save(new TradingModeEvent(requested));
        return toResponse(saved.getMode(), saved.getChangedAt(), completed);
    }

    private long successfulPaperTrades() {
        return orderRepository.countByOrderModeAndStatus(TradingMode.PAPER, OrderStatus.FILLED);
    }

    private TradingModeResponse toResponse(TradingMode mode, Instant changedAt, long completed) {
        return new TradingModeResponse(mode, changedAt, completed, paperTradeThreshold, completed >= paperTradeThreshold);
    }
}
