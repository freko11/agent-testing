package com.autotrade.dashboard.backtest;

/**
 * Win/loss/wash tally at a single {@link Checkpoint}, across every BUY/SELL decision point for
 * one {@link com.autotrade.dashboard.signal.SignalRuleId}. {@code notScored} counts decision
 * points too close to the end of the fixture series to reach this checkpoint's forward
 * horizon — tracked so total call counts aren't understated, but excluded from the win rate.
 *
 * <p>{@code avgWinReturnPct}/{@code avgLossReturnPct} (E2-F4-S2) are the average signed forward
 * return across WIN calls / LOSS calls respectively (win average is always &gt;= 0, loss average
 * always &lt;= 0, since both are derived from the same deadband classification as
 * {@link #winRate()}) — a near-coin-flip win rate can still be profitable if wins run bigger than
 * losses, which win rate alone can't show. WASH calls contribute zero to both, matching
 * {@link #expectancyPct()} treating a wash as a zero-return outcome.
 *
 * <p>{@code tpHit}/{@code slHit}/{@code horizonExpired} (E8-F2-S1) partition the same {@code
 * scored()} calls by {@link ExitReason} — a decision point resolved by a take-profit/stop-loss
 * crossing before this checkpoint's day, vs. one that fell back to the fixed-day endpoint scoring
 * this checkpoint used prior to E8-F2-S1. Always sums to {@code scored()}.
 *
 * <p>{@code avgHoldingDays} (E8-F2-S3) is the average number of days forward a scored call (win,
 * loss, or wash — a wash still means a position was held and paid funding while open) actually
 * resolved at, across every {@code scored()} call at this checkpoint — feeds {@link
 * #expectancyPctAfterCostsAndFunding()}'s duration-scaled funding cost. 0.0 when nothing at this
 * checkpoint was scored.
 *
 * <p>Promoted to main scope (E8-F5-S1) — see {@link BacktestConfig}'s class Javadoc.
 */
public record CheckpointStats(int win, int loss, int wash, int notScored, double avgWinReturnPct,
                               double avgLossReturnPct, int tpHit, int slHit, int horizonExpired,
                               double avgHoldingDays) {

    public int scored() {
        return win + loss + wash;
    }

    public double winRate() {
        return scored() == 0 ? 0.0 : (100.0 * win / scored());
    }

    /** Expected return per call (percent), win-rate-weighted by avg win/loss size with wash
     * scored as zero — the "is this branch actually worth trusting with capital" number a win
     * rate alone can't answer. 0.0 when nothing at this checkpoint was scored. */
    public double expectancyPct() {
        return scored() == 0 ? 0.0 : (avgWinReturnPct * win + avgLossReturnPct * loss) / scored();
    }

    /** {@link #expectancyPct()} after subtracting {@link BacktestConfig#TRANSACTION_COST_BPS}'s
     * flat round-trip cost (E8-F2-S2) — the number a real bracket order's expectancy would land
     * on after paying spread/slippage/fees, not just the paper price-action outcome. Cost is
     * subtracted once per trade regardless of whether it exited via TP/SL/horizon, so this is
     * exactly {@code expectancyPct() - costPct} rather than needing its own win/loss tally: since
     * {@link #expectancyPct()} already treats every WASH call as a zero-return outcome, applying
     * the flat cost to every individual scored call before re-averaging is algebraically
     * identical to subtracting it once from the aggregate. 0.0 (not a negative cost) when nothing
     * at this checkpoint was scored, matching {@link #expectancyPct()}'s own empty-checkpoint
     * guard. */
    public double expectancyPctAfterCosts() {
        return scored() == 0 ? 0.0
                : expectancyPct() - BacktestConfig.TRANSACTION_COST_BPS.doubleValue() / 100.0;
    }

    /** {@link #expectancyPctAfterCosts()} after also subtracting Binance Futures perpetual
     * funding-rate carry cost (E8-F2-S3), scaled by this checkpoint's own {@link
     * #avgHoldingDays()} rather than applied once flat like {@link BacktestConfig
     * #TRANSACTION_COST_BPS}. Exact, not an approximation: funding cost is linear in days held,
     * so {@code rate * avg(daysHeld)} over every scored call is algebraically identical to
     * netting each call's own funding cost before re-averaging — the same identity {@link
     * #expectancyPctAfterCosts()} already relies on for its flat cost, generalized to a
     * per-call-varying one. 0.0 when nothing at this checkpoint was scored. */
    public double expectancyPctAfterCostsAndFunding() {
        return scored() == 0 ? 0.0 : expectancyPctAfterCosts() - fundingCostPct();
    }

    private double fundingCostPct() {
        double periodsPerDay = 24.0 / BacktestConfig.FUNDING_PERIOD_HOURS;
        return BacktestConfig.FUNDING_RATE_BPS_PER_PERIOD.doubleValue() / 100.0 * periodsPerDay * avgHoldingDays;
    }
}
