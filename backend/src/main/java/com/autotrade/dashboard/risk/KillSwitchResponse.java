package com.autotrade.dashboard.risk;

import java.time.Instant;

/** Current kill-switch state plus when/by whom it was last changed (both null if never touched). */
public record KillSwitchResponse(KillSwitchState state, Instant changedAt, String changedBy) {
}
