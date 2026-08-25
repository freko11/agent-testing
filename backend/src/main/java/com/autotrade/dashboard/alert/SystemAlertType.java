package com.autotrade.dashboard.alert;

/** The two ops-facing events {@link SystemAlertService} records — see {@link SystemAlert}. */
public enum SystemAlertType {
    KILL_SWITCH_ENGAGED,
    SIGNAL_DRIFT_DECAY
}
