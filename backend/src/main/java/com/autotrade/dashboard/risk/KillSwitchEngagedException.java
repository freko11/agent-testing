package com.autotrade.dashboard.risk;

public class KillSwitchEngagedException extends RuntimeException {

    public KillSwitchEngagedException() {
        super("Kill switch is engaged — new order submissions are blocked until it is manually cleared.");
    }
}
