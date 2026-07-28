package com.autotrade.dashboard.signal;

/**
 * A suggested hold-term range for a BUY/SELL call (E2-F3-S2). {@code null} on
 * {@link SignalResponse} whenever the call is HOLD — there's no entry to size a horizon
 * for, so manufacturing a range would misleadingly imply HOLD is itself a timed position.
 */
public record HoldTerm(int minDays, int maxDays, String label, String rationale, String tableVersion) {

    static HoldTerm of(HoldTermRule rule, String tableVersion) {
        String label = rule.minDays() + "-" + rule.maxDays() + " days";
        return new HoldTerm(rule.minDays(), rule.maxDays(), label, rule.rationale(), tableVersion);
    }
}
