package com.autotrade.dashboard.backtest;

/**
 * Whether a HOLD call's forward price move over {@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS}
 * exceeded {@link BacktestConfig#LARGE_MOVE_THRESHOLD_PCT} — evidence for or against a
 * suppression gate (VOLATILITY_TOO_EXTREME, VOLUME_DRIED_UP, NO_VOLUME_DATA) or a
 * no-confident-vote branch (CONFLICTING_SIGNALS, NO_STRONG_SIGNAL) having been the right call
 * to suppress on.
 */
public enum HoldGateOutcome {
    LARGE_MOVE,
    STABLE
}
