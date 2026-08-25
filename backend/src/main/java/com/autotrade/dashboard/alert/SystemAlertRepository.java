package com.autotrade.dashboard.alert;

import com.autotrade.dashboard.backtest.Checkpoint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SystemAlertRepository extends JpaRepository<SystemAlert, Long> {

    List<SystemAlert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    boolean existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
            SystemAlertType alertType, String ruleTableVersion, String direction, Checkpoint checkpoint,
            Instant createdAtAfter);
}
