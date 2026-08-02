package com.autotrade.dashboard.order;

import com.autotrade.dashboard.common.TradingMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByClientOrderId(String clientOrderId);

    List<Order> findByOrderModeAndCreatedAtBetweenOrderByCreatedAtAsc(TradingMode orderMode, Instant start, Instant end);

    List<Order> findByCreatedAtBetweenOrderByCreatedAtAsc(Instant start, Instant end);

    List<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByOrderModeAndStatus(TradingMode orderMode, OrderStatus status);

    List<Order> findByStatusNotIn(Collection<OrderStatus> statuses);

    /**
     * Sum of {@code requestedAmountUsd * leverage} (notional) across this app's own currently-open orders in
     * {@code orderMode}, for E6-F2-S3's portfolio-level aggregate exposure cap. Scoped to a single trading mode —
     * paper and live are separate broker accounts/capital pools, so aggregating across both would misrepresent real
     * risk, matching {@link #countByOrderModeAndStatus}'s mode-scoped precedent. {@code COALESCE} guarantees a
     * non-null zero with no open orders, so callers never need a null check.
     */
    @Query("SELECT COALESCE(SUM(o.requestedAmountUsd * o.leverage), 0) FROM Order o "
            + "WHERE o.orderMode = :orderMode AND o.status NOT IN :excludedStatuses")
    BigDecimal sumOpenNotionalUsd(@Param("orderMode") TradingMode orderMode,
                                  @Param("excludedStatuses") Collection<OrderStatus> excludedStatuses);
}
