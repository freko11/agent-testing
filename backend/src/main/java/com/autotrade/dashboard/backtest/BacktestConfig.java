package com.autotrade.dashboard.backtest;

import java.math.BigDecimal;

/**
 * Diagnostic thresholds used to measure backtest outcomes (E2-F4-S1) — deliberately separate
 * from and NOT versioned with {@link com.autotrade.dashboard.signal.SignalRuleEngine#RULE_TABLE_VERSION}
 * or {@link com.autotrade.dashboard.signal.HoldTermCalculator#HOLD_TERM_TABLE_VERSION}, since
 * these measure outcomes rather than define the rule table under test.
 *
 * <p>Promoted to main scope (E8-F5-S1) from {@code src/test/java} — originally a pure test-only
 * fixture-scoring helper, now also reused by {@code monitoring.LiveSignalDriftService} to score
 * real forward market data the same way {@code backtest.BacktestHarness} scores a fixture.
 */
public final class BacktestConfig {

    /** A forward move smaller than this (in either direction, relative to the called direction)
     * counts as a WASH rather than a WIN or LOSS. */
    public static final BigDecimal WIN_LOSS_DEADBAND_PCT = new BigDecimal("0.25");

    /** Fixed reference horizon (in candle-index steps) used to score every HOLD call, regardless
     * of which rule matched — HOLD calls carry no hold-term of their own to derive a horizon from. */
    public static final int HOLD_REFERENCE_HORIZON_DAYS = 5;

    /** A forward move (absolute value) beyond this over the reference horizon counts as "large". */
    public static final BigDecimal LARGE_MOVE_THRESHOLD_PCT = new BigDecimal("3.0");

    /**
     * Take-profit / stop-loss distance (percent of decision-close price) the day-by-day
     * walk-forward scan (E8-F2-S1) checks each candle's high/low against. Unlike this class's
     * other constants, these aren't derived from anything in the signal or order path: {@code
     * PlaceOrderRequest.takeProfitPrice}/{@code stopLossPrice} are free-form user-entered
     * absolute prices with no percentage relationship to the signal, and neither the frontend
     * trade form nor the rule table suggests a default. These are a representative,
     * deliberately uncalibrated placeholder bracket (5% target / 3% stop) — the same treatment
     * as this file's other diagnostic thresholds, pending a future calibration pass akin to
     * E8-F1-S1's threshold sweep.
     */
    public static final BigDecimal TAKE_PROFIT_PCT = new BigDecimal("5.0");
    public static final BigDecimal STOP_LOSS_PCT = new BigDecimal("3.0");

    /**
     * Round-trip transaction cost (spread + slippage + fees for entry AND exit combined), in
     * basis points, subtracted from every scored outcome's expectancy (E8-F2-S2). Like {@link
     * #TAKE_PROFIT_PCT}/{@link #STOP_LOSS_PCT}, this is a deliberately uncalibrated placeholder:
     * Binance Futures taker fees run ~10bps round trip, with an added ~10bps slippage buffer
     * biased toward DOGEUSDT's (the smaller-cap of the two checked-in fixtures) worse execution
     * quality rather than BTCUSDT's, since overstating cost is the safer failure mode for a
     * story about not overstating paper profitability. A single flat value across both fixtures,
     * not asset-differentiated — the harness carries no asset-type parameter through its call
     * chain today, so a per-symbol figure would be a materially larger change than this constant.
     * Excludes Binance Futures funding-rate carry cost (paid periodically, scales with hold
     * duration rather than being a flat one-time cost) — out of scope per this story's AC, which
     * covers spread/slippage/fees only.
     */
    public static final BigDecimal TRANSACTION_COST_BPS = new BigDecimal("20");

    private BacktestConfig() {
    }
}
