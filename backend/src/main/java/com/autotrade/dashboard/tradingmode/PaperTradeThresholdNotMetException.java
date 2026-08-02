package com.autotrade.dashboard.tradingmode;

/**
 * Thrown by {@link TradingModeService#switchTo} when a switch to {@code
 * LIVE} is requested before the configured minimum number of successful
 * (filled, paper-mode) orders has been reached (E6-F1-S2). Distinct from
 * {@code E6-F1-S3}'s future risk-consent gate — this one is purely
 * data-driven and resolves itself as more paper trades fill.
 */
public class PaperTradeThresholdNotMetException extends RuntimeException {

    private final long completed;
    private final int required;

    public PaperTradeThresholdNotMetException(long completed, int required) {
        super("Live mode requires " + required + " successful paper trades; you've completed " + completed + ".");
        this.completed = completed;
        this.required = required;
    }

    public long getCompleted() {
        return completed;
    }

    public int getRequired() {
        return required;
    }
}
