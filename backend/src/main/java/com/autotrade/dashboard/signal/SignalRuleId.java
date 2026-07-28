package com.autotrade.dashboard.signal;

/**
 * Every branch of {@link SignalRuleEngine}'s rule table, in priority order (declaration
 * order below matches evaluation order — the first matching rule wins). Carrying the
 * matched rule id (not just the resulting {@link SignalCall}) makes every call directly
 * auditable: which named rule fired, and why. Feeds E6-F3-S2's future rule-provenance
 * audit-log requirement.
 */
public enum SignalRuleId {

    /** Volume-trend is null (a dead/zero-volume ticker) — highest priority, forces HOLD before RSI/MACD/MA are even inspected. */
    NO_VOLUME_DATA(SignalCall.HOLD, "No reliable volume data for this ticker; call suppressed."),

    /** 10-day volume has dried up to under 20% of the 30-day baseline — a call here isn't trustworthy. */
    VOLUME_DRIED_UP(SignalCall.HOLD, "Volume has dried up relative to its 30-day baseline; call suppressed."),

    /** ATR% is above the extreme-volatility threshold — too erratic to call with confidence. */
    VOLATILITY_TOO_EXTREME(SignalCall.HOLD, "Volatility is too extreme for a confident call."),

    /** All three of RSI/MACD/MA agree bullish, none dissent. */
    BULLISH_UNANIMOUS(SignalCall.BUY, "RSI, MACD, and moving-average crossover are all bullish."),

    /** Two of three agree bullish, the third neutral (not dissenting). */
    BULLISH_MAJORITY(SignalCall.BUY, "A majority of indicators (RSI/MACD/MA) are bullish, with no dissent."),

    /** All three of RSI/MACD/MA agree bearish, none dissent. */
    BEARISH_UNANIMOUS(SignalCall.SELL, "RSI, MACD, and moving-average crossover are all bearish."),

    /** Two of three agree bearish, the third neutral (not dissenting). */
    BEARISH_MAJORITY(SignalCall.SELL, "A majority of indicators (RSI/MACD/MA) are bearish, with no dissent."),

    /** At least one indicator is bullish and at least one is bearish — no reliable direction. */
    CONFLICTING_SIGNALS(SignalCall.HOLD, "Indicators disagree on direction; call suppressed."),

    /** Zero or one indicator has a directional read, with no opposition — not enough signal to call either way. */
    NO_STRONG_SIGNAL(SignalCall.HOLD, "No strong directional signal from RSI, MACD, or moving-average crossover.");

    private final SignalCall call;
    private final String rationale;

    SignalRuleId(SignalCall call, String rationale) {
        this.call = call;
        this.rationale = rationale;
    }

    public SignalCall call() {
        return call;
    }

    public String rationale() {
        return rationale;
    }
}
