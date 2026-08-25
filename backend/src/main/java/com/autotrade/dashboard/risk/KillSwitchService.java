package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.alert.SystemAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The global kill switch (E6-F2-S2) — read by {@code OrderService.submitOrder} as a
 * pre-flight gate, same append-only-event/"latest row = current state" pattern as {@code
 * TradingModeService}. Defaults to {@link KillSwitchState#CLEARED} on a fresh install with
 * no history, same "no history = safe default" convention. Switching to the state already
 * active is an idempotent no-op (no new history row), matching {@code
 * TradingModeService.switchTo}.
 */
@Service
public class KillSwitchService {

    private static final Logger log = LoggerFactory.getLogger(KillSwitchService.class);

    private final KillSwitchEventRepository repository;
    private final SystemAlertService systemAlertService;

    public KillSwitchService(KillSwitchEventRepository repository, SystemAlertService systemAlertService) {
        this.repository = repository;
        this.systemAlertService = systemAlertService;
    }

    public KillSwitchResponse currentState() {
        return repository.findTopByOrderByIdDesc()
                .map(event -> new KillSwitchResponse(event.getState(), event.getChangedAt(), event.getChangedBy()))
                .orElse(new KillSwitchResponse(KillSwitchState.CLEARED, null, null));
    }

    public boolean isEngaged() {
        return currentState().state() == KillSwitchState.ENGAGED;
    }

    public void assertNotEngaged() {
        if (isEngaged()) {
            throw new KillSwitchEngagedException();
        }
    }

    public KillSwitchResponse engage(String changedBy) {
        return switchTo(KillSwitchState.ENGAGED, changedBy);
    }

    public KillSwitchResponse clear(String changedBy) {
        return switchTo(KillSwitchState.CLEARED, changedBy);
    }

    private KillSwitchResponse switchTo(KillSwitchState requested, String changedBy) {
        KillSwitchResponse current = currentState();
        if (current.state() == requested) {
            return current;
        }
        KillSwitchEvent saved = repository.save(new KillSwitchEvent(requested, changedBy));
        log.warn("Kill switch {} -> {} by '{}'", current.state(), saved.getState(), changedBy);
        if (requested == KillSwitchState.ENGAGED) {
            systemAlertService.recordKillSwitchEngaged(saved);
        }
        return new KillSwitchResponse(saved.getState(), saved.getChangedAt(), saved.getChangedBy());
    }
}
