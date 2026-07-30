package com.autotrade.dashboard.watchlist;

import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import com.autotrade.dashboard.ticker.TickerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sole entry point for the watchlist (E3-F3-S1). A ticker must already be
 * registered (see {@link TickerService#findRegistered}) before it can be
 * watchlisted — same "explicit registration first" precedent as market
 * data/indicators/signal, reusing {@code TICKER_NOT_REGISTERED} rather than
 * inventing a parallel error code.
 */
@Service
public class WatchlistService {

    private final WatchlistEntryRepository repository;
    private final TickerService tickerService;

    public WatchlistService(WatchlistEntryRepository repository, TickerService tickerService) {
        this.repository = repository;
        this.tickerService = tickerService;
    }

    @Transactional(readOnly = true)
    public List<WatchlistEntry> list() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    /** For {@code WatchlistSignalPoller} (E5-F4-S1) — returns real {@link Ticker} entities (not the lazy {@code
     * WatchlistEntry.ticker} association), safe to use after this transaction closes. */
    @Transactional(readOnly = true)
    public List<Ticker> listTickers() {
        return repository.findAllWatchlistedTickersOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public boolean contains(String symbol) {
        Ticker ticker = requireRegistered(symbol);
        return repository.existsByTicker_Id(ticker.getId());
    }

    /**
     * Adds a ticker to the watchlist, or returns the existing entry if it's
     * already saved. Idempotent by design, same as
     * {@link TickerService#resolveOrRegister}.
     */
    @Transactional
    public WatchlistEntry add(String symbol) {
        Ticker ticker = requireRegistered(symbol);
        return repository.findByTicker_Id(ticker.getId())
                .orElseGet(() -> repository.save(new WatchlistEntry(ticker)));
    }

    /**
     * Removes a ticker from the watchlist. A no-op (not an error) if it
     * wasn't on the watchlist to begin with — DELETE is idempotent.
     */
    @Transactional
    public void remove(String symbol) {
        Ticker ticker = requireRegistered(symbol);
        repository.findByTicker_Id(ticker.getId()).ifPresent(repository::delete);
    }

    private Ticker requireRegistered(String symbol) {
        return tickerService.findRegistered(symbol)
                .orElseThrow(() -> new TickerNotRegisteredException(normalize(symbol)));
    }

    private String normalize(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase();
    }
}
