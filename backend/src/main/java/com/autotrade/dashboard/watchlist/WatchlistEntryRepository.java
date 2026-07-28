package com.autotrade.dashboard.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistEntryRepository extends JpaRepository<WatchlistEntry, Long> {

    List<WatchlistEntry> findAllByOrderByCreatedAtDesc();

    Optional<WatchlistEntry> findByTicker_Id(Long tickerId);

    boolean existsByTicker_Id(Long tickerId);
}
