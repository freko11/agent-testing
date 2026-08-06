package com.autotrade.dashboard.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand live signal-drift check (E8-F5-S1) — session-authenticated the same as every other
 * endpoint in this app (no new carve-out in {@code SecurityConfig}, which already requires
 * authentication on any request not explicitly permitted). {@code lookbackDays} lets an operator
 * widen or narrow the replay window without waiting for the next {@code @Scheduled} run; omitted,
 * it falls back to the configured {@code monitoring.live-drift.lookback-days} default.
 *
 * <p>Gated by the same {@code monitoring.live-drift.enabled} flag as {@link
 * LiveSignalDriftService} (rather than only the scheduled job) — the service bean this controller
 * depends on doesn't exist at all when the flag is off (e.g. the test profile, which forces it
 * false to keep {@code @SpringBootTest} from ever making a real market-data call), so the
 * controller has to disappear together with it rather than fail to wire.
 */
@RestController
@RequestMapping("/api/monitoring")
@ConditionalOnProperty(name = "monitoring.live-drift.enabled", havingValue = "true", matchIfMissing = true)
public class SignalDriftController {

    private final LiveSignalDriftService liveSignalDriftService;

    public SignalDriftController(LiveSignalDriftService liveSignalDriftService) {
        this.liveSignalDriftService = liveSignalDriftService;
    }

    @GetMapping("/signal-drift")
    public SignalDriftReport signalDrift(@RequestParam(name = "lookbackDays", required = false) Integer lookbackDays) {
        return lookbackDays != null ? liveSignalDriftService.computeDrift(lookbackDays) : liveSignalDriftService.computeDrift();
    }
}
