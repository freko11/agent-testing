package com.autotrade.dashboard.alert;

import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.risk.KillSwitchEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Records ops-facing system alerts for two independent triggers: the kill switch engaging
 * (hooked from {@code risk.KillSwitchService.switchTo}) and live signal drift crossing into
 * {@code possibleDecay} (hooked from {@code monitoring.LiveSignalDriftService}'s scheduled
 * job only — never the on-demand controller path, so opening the dashboard never itself
 * creates an alert). Every {@code record*} method swallows and logs its own failures — an
 * alert-recording bug must never fail the kill-switch response or abort the scheduled drift
 * job, same convention as {@code notification.NotificationService}.
 */
@Service
public class SystemAlertService {

    private static final Logger log = LoggerFactory.getLogger(SystemAlertService.class);

    private final SystemAlertRepository repository;
    private final Clock clock;
    private final long reAlertCooldownMs;

    public SystemAlertService(SystemAlertRepository repository, Clock clock,
                               @Value("${alert.signal-drift-decay.re-alert-cooldown-ms}") long reAlertCooldownMs) {
        this.repository = repository;
        this.clock = clock;
        this.reAlertCooldownMs = reAlertCooldownMs;
    }

    /**
     * No dedupe needed: {@code KillSwitchService.switchTo} already returns early (no save, no
     * call to this method) when the requested state matches the current one, so repeated
     * {@code engage()} calls while already engaged never reach here.
     */
    public void recordKillSwitchEngaged(KillSwitchEvent event) {
        try {
            repository.save(SystemAlert.forKillSwitchEngaged(event));
        } catch (RuntimeException e) {
            log.warn("Failed to record kill-switch-engaged system alert for event {}", event.getId(), e);
        }
    }

    /**
     * Skips the insert if a {@code SIGNAL_DRIFT_DECAY} alert for the exact same {@code
     * (ruleTableVersion, direction, checkpoint)} triple was already recorded within the
     * configured cooldown window — re-surfaces a still-decaying condition periodically rather
     * than spamming one alert per scheduled run or going silent after the first one.
     */
    public void recordSignalDriftDecay(String ruleTableVersion, String direction, Checkpoint checkpoint,
                                        double driftPct) {
        try {
            Instant cutoff = clock.instant().minusMillis(reAlertCooldownMs);
            boolean alreadyAlerted = repository
                    .existsByAlertTypeAndRuleTableVersionAndDirectionAndCheckpointAndCreatedAtAfter(
                            SystemAlertType.SIGNAL_DRIFT_DECAY, ruleTableVersion, direction, checkpoint, cutoff);
            if (alreadyAlerted) {
                return;
            }
            repository.save(SystemAlert.forSignalDriftDecay(ruleTableVersion, direction, checkpoint, driftPct));
        } catch (RuntimeException e) {
            log.warn("Failed to record signal-drift-decay system alert for ruleTableVersion={} direction={} checkpoint={}",
                    ruleTableVersion, direction, checkpoint, e);
        }
    }

    public List<SystemAlert> list(int limit) {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
    }
}
