package com.autotrade.dashboard.signal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SignalCallEntryRepository extends JpaRepository<SignalCallEntry, Long> {

    /** Batched lookup (avoids N+1) for CSV export (E5-F3-S2), keyed by the {@code IndicatorSnapshot} an {@code Order}
     * was placed against — not DB-enforced 1:1, so a caller with multiple matches per snapshot should pick one. */
    List<SignalCallEntry> findByIndicatorSnapshot_IdIn(Collection<Long> indicatorSnapshotIds);
}
