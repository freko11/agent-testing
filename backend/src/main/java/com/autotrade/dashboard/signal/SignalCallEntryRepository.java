package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.ticker.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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

    /** Single-snapshot version of {@link #findByIndicatorSnapshot_IdIn} -- looked up by {@code OrderService} (E6-F3-S1)
     * right after a signal computation to FK the resulting {@code OrderAuditEntry} to the {@code SignalCallEntry} that
     * was just persisted for it. Same "not DB-enforced 1:1, tie-break to the highest id" tolerance as that method,
     * matching the pattern {@code OrderCsvExporter} already applies manually for the same ambiguity. */
    Optional<SignalCallEntry> findTopByIndicatorSnapshot_IdOrderByIdDesc(Long indicatorSnapshotId);

    /**
     * Every call logged after {@code cutoff}, oldest first (E8-F5-S3: {@code
     * monitoring.WeightedVoteShadowScoringService}'s lookback-window replay). {@code JOIN FETCH}es
     * {@code ticker}/{@code indicatorSnapshot} eagerly, same "sidestep a lazy-init-after-return
     * exception under a scheduled job's self-invocation" reasoning as {@code
     * OrderAuditEntryRepository#findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc}'s own
     * Javadoc — both fields are exactly what a shadow-scoring replay needs to reconstruct {@code
     * WeightedVoteRuleEngine.evaluate}'s inputs and batch price-history fetches per ticker symbol.
     * Deliberately unfiltered by {@link SignalCall} (BUY/SELL/HOLD) or {@code matchedRule} — every
     * persisted call, including safety-gate/no-strong-signal HOLDs, is a legitimate agreement-check
     * candidate for that story's AC.
     */
    @Query("SELECT s FROM SignalCallEntry s "
            + "JOIN FETCH s.ticker "
            + "JOIN FETCH s.indicatorSnapshot "
            + "WHERE s.createdAt > :cutoff "
            + "ORDER BY s.createdAt ASC")
    List<SignalCallEntry> findByCreatedAtAfterOrderByCreatedAtAsc(@Param("cutoff") Instant cutoff);
}
