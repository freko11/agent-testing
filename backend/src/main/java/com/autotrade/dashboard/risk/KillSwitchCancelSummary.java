package com.autotrade.dashboard.risk;

import java.util.List;

/** Outcome of one cancel-all sweep ({@code OrderService.cancelAllOpenOrders}) — per-order failures are
 * collected rather than aborting the sweep, so one broker's outage doesn't block the other's cancellations. */
public record KillSwitchCancelSummary(int attempted, int cancelled, int failed, List<String> failureMessages) {
}
