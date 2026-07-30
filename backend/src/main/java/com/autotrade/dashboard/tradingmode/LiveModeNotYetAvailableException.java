package com.autotrade.dashboard.tradingmode;

/**
 * Thrown whenever {@link TradingModeService#switchTo} is asked to switch to
 * {@code LIVE} — unconditionally, for now. This is deliberate, temporary
 * scaffolding: E6-F1-S2 (paper-trade threshold) and E6-F1-S3 (risk consent)
 * are meant to <em>replace</em> this unconditional guard with a real gate,
 * not merely delete it. Relying on "no LIVE broker credentials are seeded
 * anywhere yet" as the only backstop would be safety-by-accident rather than
 * a designed control.
 */
public class LiveModeNotYetAvailableException extends RuntimeException {

    public LiveModeNotYetAvailableException() {
        super("LIVE mode isn't available yet — it unlocks once the paper-trade threshold (E6-F1-S2) and "
                + "risk-consent step (E6-F1-S3) are in place.");
    }
}
