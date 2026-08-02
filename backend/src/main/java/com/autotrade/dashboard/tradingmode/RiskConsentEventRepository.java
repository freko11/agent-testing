package com.autotrade.dashboard.tradingmode;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskConsentEventRepository extends JpaRepository<RiskConsentEvent, Long> {

    Optional<RiskConsentEvent> findTopByOrderByIdDesc();
}
