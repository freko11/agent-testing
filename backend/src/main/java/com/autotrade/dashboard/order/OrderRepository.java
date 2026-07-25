package com.autotrade.dashboard.order;

import com.autotrade.dashboard.common.TradingMode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByClientOrderId(String clientOrderId);

    List<Order> findByOrderModeAndCreatedAtBetween(TradingMode orderMode, Instant start, Instant end);
}
