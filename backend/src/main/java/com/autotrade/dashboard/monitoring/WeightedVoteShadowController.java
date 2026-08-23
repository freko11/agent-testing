package com.autotrade.dashboard.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand weighted-vote shadow-scoring check (E8-F5-S3) — session-authenticated the same as
 * every other endpoint in this app, no new carve-out in {@code SecurityConfig}. {@code
 * lookbackDays} lets an operator widen or narrow the replay window without waiting for the next
 * {@code @Scheduled} run; omitted, it falls back to the configured {@code
 * monitoring.weighted-vote-shadow.lookback-days} default. Mirrors {@link SignalDriftController}'s
 * own shape exactly.
 *
 * <p>Gated by the same {@code monitoring.weighted-vote-shadow.enabled} flag as {@link
 * WeightedVoteShadowScoringService} (rather than only the scheduled job) — the service bean this
 * controller depends on doesn't exist at all when the flag is off (e.g. the test profile, which
 * forces it false to keep {@code @SpringBootTest} from ever making a real market-data call), so
 * the controller has to disappear together with it rather than fail to wire.
 */
@RestController
@RequestMapping("/api/monitoring")
@ConditionalOnProperty(name = "monitoring.weighted-vote-shadow.enabled", havingValue = "true", matchIfMissing = true)
public class WeightedVoteShadowController {

    private final WeightedVoteShadowScoringService weightedVoteShadowScoringService;

    public WeightedVoteShadowController(WeightedVoteShadowScoringService weightedVoteShadowScoringService) {
        this.weightedVoteShadowScoringService = weightedVoteShadowScoringService;
    }

    @GetMapping("/weighted-vote-shadow")
    public WeightedVoteShadowReport weightedVoteShadow(
            @RequestParam(name = "lookbackDays", required = false) Integer lookbackDays) {
        return lookbackDays != null
                ? weightedVoteShadowScoringService.computeShadowReport(lookbackDays)
                : weightedVoteShadowScoringService.computeShadowReport();
    }
}
