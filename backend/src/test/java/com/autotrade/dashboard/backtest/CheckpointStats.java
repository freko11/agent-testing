package com.autotrade.dashboard.backtest;

/**
 * Win/loss/wash tally at a single {@link Checkpoint}, across every BUY/SELL decision point for
 * one {@link com.autotrade.dashboard.signal.SignalRuleId}. {@code notScored} counts decision
 * points too close to the end of the fixture series to reach this checkpoint's forward
 * horizon — tracked so total call counts aren't understated, but excluded from the win rate.
 */
public record CheckpointStats(int win, int loss, int wash, int notScored) {

    public int scored() {
        return win + loss + wash;
    }

    public double winRate() {
        return scored() == 0 ? 0.0 : (100.0 * win / scored());
    }
}
