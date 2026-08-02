package com.autotrade.dashboard.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KillSwitchEventRepository extends JpaRepository<KillSwitchEvent, Long> {

    Optional<KillSwitchEvent> findTopByOrderByIdDesc();
}
