package com.autotrade.dashboard.ticker;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TickerRepository extends JpaRepository<Ticker, Long> {
}
