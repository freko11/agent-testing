package com.autotrade.dashboard.risk;

/** Combined response for engaging the kill switch — the new state plus what happened to open orders. */
public record EngageKillSwitchResponse(KillSwitchResponse killSwitch, KillSwitchCancelSummary cancelSummary) {
}
