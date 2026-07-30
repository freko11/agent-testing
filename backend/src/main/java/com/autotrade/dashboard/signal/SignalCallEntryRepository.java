package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.ticker.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SignalCallEntryRepository extends JpaRepository<SignalCallEntry, Long> {

    /** Batched lookup (avoids N+1) for CSV export (E5-F3-S2), keyed by the {@code IndicatorSnapshot} an {@code Order}
     * was placed against — not DB-enforced 1:1, so a caller with multiple matches per snapshot should pick one. */
    List<SignalCallEntry> findByIndicatorSnapshot_IdIn(Collection<Long> indicatorSnapshotIds);

    /** The most recent call for a ticker, used by {@code WatchlistSignalPoller} (E5-F4-S1) as the "previous known
     * call" baseline — reuses this existing append-only audit table instead of a new "last known state" table, and
     * is restart-safe for free since it's a real persisted row, not in-memory state. {@code id desc} breaks ties on
     * {@code created_at} collisions (same-instant saves). */
    Optional<SignalCallEntry> findTopByTickerOrderByCreatedAtDescIdDesc(Ticker ticker);
}
