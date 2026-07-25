package com.autotrade.dashboard.indicator;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IndicatorSnapshotRepository extends JpaRepository<IndicatorSnapshot, Long> {

    Optional<IndicatorSnapshot> findFirstByTickerIdOrderBySnapshotAtDesc(Long tickerId);
}
