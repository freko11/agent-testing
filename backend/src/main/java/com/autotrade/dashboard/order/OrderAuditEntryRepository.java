package com.autotrade.dashboard.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderAuditEntryRepository extends JpaRepository<OrderAuditEntry, Long> {

    /**
     * Newest-first page of every audit entry, for the audit-trail viewer (E6-F3-S3) -- unlike
     * {@link #findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc} below, this is unfiltered by
     * {@code resultStatus} (a rejected/failed order is exactly the kind of row a review UI needs to
     * surface, not just filled ones) and paginated rather than a bounded time-window scan. {@code
     * JOIN FETCH}es {@code order}/{@code ticker}/{@code signalCallEntry} for the same reason as the
     * query below -- avoids a lazy-init exception on those associations once the transaction that
     * loaded them has closed. An explicit {@code countQuery} avoids relying on Spring Data to strip
     * {@code JOIN FETCH} out of an auto-derived count query, since the join targets here are all
     * to-one associations and don't affect the row count either way.
     */
    @Query(value = "SELECT a FROM OrderAuditEntry a "
            + "JOIN FETCH a.order "
            + "JOIN FETCH a.ticker "
            + "JOIN FETCH a.signalCallEntry "
            + "ORDER BY a.loggedAt DESC",
            countQuery = "SELECT COUNT(a) FROM OrderAuditEntry a")
    Page<OrderAuditEntry> findAllByOrderByLoggedAtDesc(Pageable pageable);

    /**
     * Audit entries whose entry leg actually filled (E8-F5-S1: {@code resultStatus} in {@code
     * {FILLED, PARTIALLY_PROTECTED}} — both mean real market exposure existed, unlike {@code
     * REJECTED}/{@code FAILED}/{@code SUBMISSION_UNKNOWN}, which never reached the broker or
     * never resolved), logged after {@code cutoff}, oldest first — the replay order {@code
     * monitoring.LiveSignalDriftService} scores in.
     *
     * <p>{@code JOIN FETCH}es {@code ticker}/{@code signalCallEntry}/{@code
     * signalCallEntry.indicatorSnapshot} eagerly rather than relying on {@code @Transactional} to
     * keep those lazy associations alive — {@code LiveSignalDriftService}'s {@code @Scheduled}
     * method calls {@code computeDrift} via plain self-invocation (same class), which bypasses
     * Spring's transactional proxy entirely, so a lazy-load-after-return
     * {@code LazyInitializationException} (this codebase's own documented recurring gotcha, see
     * CLAUDE.md) would otherwise be a real risk here specifically. A join fetch sidesteps the
     * issue rather than working around the self-invocation limitation.
     */
    @Query("SELECT a FROM OrderAuditEntry a "
            + "JOIN FETCH a.ticker "
            + "JOIN FETCH a.signalCallEntry sce "
            + "JOIN FETCH sce.indicatorSnapshot "
            + "WHERE a.resultStatus IN :resultStatuses AND a.loggedAt > :cutoff "
            + "ORDER BY a.loggedAt ASC")
    List<OrderAuditEntry> findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(
            @Param("resultStatuses") Collection<OrderStatus> resultStatuses, @Param("cutoff") Instant cutoff);
}
