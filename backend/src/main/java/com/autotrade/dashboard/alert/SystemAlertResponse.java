package com.autotrade.dashboard.alert;

import com.autotrade.dashboard.backtest.Checkpoint;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemAlertResponse(
        Long id,
        SystemAlertType alertType,
        String message,
        String ruleTableVersion,
        String direction,
        Checkpoint checkpoint,
        Double driftPct,
        Instant createdAt) {

    static SystemAlertResponse from(SystemAlert alert) {
        return new SystemAlertResponse(alert.getId(), alert.getAlertType(), alert.getMessage(),
                alert.getRuleTableVersion(), alert.getDirection(), alert.getCheckpoint(), alert.getDriftPct(),
                alert.getCreatedAt());
    }
}
