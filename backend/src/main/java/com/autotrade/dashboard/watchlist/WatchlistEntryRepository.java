package com.autotrade.dashboard.watchlist;

import com.autotrade.dashboard.ticker.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, Long> {

    List<WatchlistEntry> findAllByOrderByCreatedAtDesc();

    Optional<WatchlistEntry> findByTicker_Id(Long tickerId);

    boolean existsByTicker_Id(Long tickerId);

    /** A real join, not the lazy {@code WatchlistEntry.ticker} association — returns fully-loaded {@link Ticker}
     * entities usable after the transaction closes, for {@code WatchlistSignalPoller} (E5-F4-S1), which accesses
     * tickers outside any transaction (it must not hold a DB session open across its market-data/signal HTTP calls,
     * same discipline as {@code OrderService}'s broker calls). */
    @Query("select w.ticker from WatchlistEntry w order by w.createdAt desc")
    List<Ticker> findAllWatchlistedTickersOrderByCreatedAtDesc();
}
