package com.autotrade.dashboard.ticker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Sole entry point for resolving/registering tickers. A ticker must be
 * explicitly registered with its {@link AssetType} before market data can be
 * fetched for it (E2-F1-S1 design gate) — the app never guesses stock vs.
 * crypto from symbol shape, since {@code Ticker.assetType} is already a
 * mandatory schema column and this app favors deterministic rules over
 * fuzzy inference everywhere else (rule engine, guardrails).
 */
@Service
public class TickerService {

    private final TickerRepository repository;

    public TickerService(TickerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Optional<Ticker> findRegistered(String symbol) {
        return repository.findBySymbol(normalize(symbol));
    }

    /**
     * Registers a new ticker, or returns the existing one if the symbol is
     * already registered with the same asset type. Idempotent by design —
     * re-registering an unchanged ticker is not an error.
     */
    @Transactional
    public Ticker resolveOrRegister(String symbol, AssetType assetType, String exchange) {
        String normalized = normalize(symbol);
        Optional<Ticker> existing = repository.findBySymbol(normalized);
        if (existing.isPresent()) {
            Ticker ticker = existing.get();
            if (ticker.getAssetType() != assetType) {
                throw new TickerAssetTypeConflictException(normalized, ticker.getAssetType(), assetType);
            }
            return ticker;
        }
        return repository.save(new Ticker(normalized, assetType, exchange));
    }

    private String normalize(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase();
    }
}
