package com.autotrade.dashboard.tradingmode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TradingModeEventRepository extends JpaRepository<TradingModeEvent, Long> {

    Optional<TradingModeEvent> findTopByOrderByIdDesc();
}
