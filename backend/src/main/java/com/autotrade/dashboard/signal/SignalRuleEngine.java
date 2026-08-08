package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;

import java.math.BigDecimal;

/**
 * Deterministic Buy/Sell/Hold rule table (E2-F3-S1): three safety gates (null/dried-up
 * volume, extreme volatility) run first and can only ever force HOLD, then a 2-of-3
 * directional vote across RSI/MACD/MA-crossover with no dissent allowed decides BUY/SELL.
 * Rules are evaluated in {@link SignalRuleId} declaration order; the first match wins.
 *
 * <p>RSI 25/75 and the two gate thresholds were backtest-calibrated (E8-F1-S1) by sweeping
 * candidates through {@code BacktestHarness} against the checked-in BTCUSDT/DOGEUSDT
 * fixtures ({@code ThresholdCalibrationTest}) — see docs/CHANGELOG.md's E8-F1-S1 entry for
 * the full candidate grid and expectancy numbers. That pass moved RSI from the original
 * hand-picked 30/70 to 25/75: widening the neutral band consistently raised win rate and
 * expectancy on the BUY side across both fixtures and every checkpoint, with a *larger*
 * sample at each candidate (fewer RSI-vs-other-indicator conflicts got resolved to HOLD),
 * so the gain isn't a small-n artifact. {@link #VOLATILITY_EXTREME_THRESHOLD} and
 * {@link #VOLUME_DRIED_UP_THRESHOLD} showed no comparable signal in the swept range and were
 * left unchanged — the only volatility candidate that looked better (5.0) did so on an n=10
 * sample, not enough to trust.
 *
 * <p><b>E8-F4-S1's out-of-sample validation found this shift does not replicate uniformly:</b>
 * the SELL-side widening holds on held-out BTCUSDT/DOGEUSDT tails and a genuinely untouched
 * SOLUSDT fixture, but the BUY-side widening — an equally central part of the original
 * finding above — reverses on two of those three out-of-sample checks, including SOLUSDT
 * (see {@code OutOfSampleValidationTest} and docs/CHANGELOG.md's E8-F4-S1 entry for the exact
 * figures). Per that story's confirmed report-only scope, RSI stays at 25/75/v2 here — the
 * BUY-side mismatch is a flagged finding for a future recalibration (e.g. asymmetric bounds),
 * not acted on yet. {@link #RULE_TABLE_VERSION} exists so revising these thresholds later is
 * an auditable, versioned change (feeds E6-F3-S2).
 *
 * <p><b>E8-F1-S2 investigated that flagged asymmetric-bounds fix and found it doesn't work.</b>
 * {@code RsiOversoldRecalibrationTest} swept {@code rsiOversold} candidates 24 through 32 —
 * holding {@code rsiOverbought} fixed at 75 — against a tuning window and then all three
 * out-of-sample surfaces (BTCUSDT/DOGEUSDT held-out tails, untouched SOLUSDT). Two findings,
 * together explaining why "revert rsiOversold to 30" is not the fix E8-F4-S1 hoped for: (1)
 * every candidate in that range produces byte-identical BUY-side outcomes on every surface —
 * {@code rsiOversold} has no measurable effect on BUY-side classification in this data at all,
 * meaning the BUY-side improvement E8-F1-S1 originally attributed to the 30&rarr;25 oversold
 * move was actually a knock-on effect of the 70&rarr;75 overbought move (a wider overbought
 * band removes RSI-bearish dissent votes on some bullish-leaning days — that's what let more
 * BUY calls through, not the oversold threshold); so there is no candidate in this range that
 * improves the BUY side E8-F4-S1 flagged. (2) {@code rsiOversold} does measurably affect the
 * SELL side (RSI-bullish votes can suppress a would-be SELL call into {@code
 * CONFLICTING_SIGNALS}), and raising it back to 30 makes SELL-side after-cost expectancy worse
 * on BTCUSDT and SOLUSDT at every checkpoint (mixed on DOGEUSDT) versus the current 25 — so
 * reverting would only trade an already-working SELL side for zero BUY-side benefit. Net:
 * nothing ships. RSI stays 25/75/v2; see docs/CHANGELOG.md's E8-F1-S2 entry for the full
 * figures. The original E8-F4-S1 BUY-side mismatch remains open — not fixable via {@code
 * rsiOversold} alone, per this finding.
 *
 * <p><b>E8-F1-S3 isolated {@code rsiOverbought} — the lever E8-F1-S2 traced the original
 * BUY-side gain to — and found it doesn't fix the mismatch either, for a new reason.</b>
 * {@code RsiOverboughtRecalibrationTest} swept {@code rsiOverbought} candidates 68 through 76 —
 * holding {@code rsiOversold} fixed at 25 — against the same tuning window and three
 * out-of-sample surfaces. Unlike {@code rsiOversold}, {@code rsiOverbought} does measurably
 * affect the BUY side (confirming E8-F1-S2's hypothesis) and, mirroring that finding in reverse,
 * has zero measurable effect on the SELL side in this data (exactly 5 distinct SELL-side result
 * lines across all 8 candidates &times; 3 fixtures) — each RSI bound only ever moves the vote
 * count on its own opposing side's dissent, never the other rule branch. But the BUY-side effect
 * is asset-dependent in a way that blocks any single fix: against the actual pre-tuning 30/70
 * baseline's out-of-sample BUY-side expectancy, BTCUSDT improves as {@code rsiOverbought} is
 * lowered toward 68, DOGEUSDT improves as it's raised toward 75/76, and SOLUSDT is best near the
 * pre-tuning value of 70 and degrades at both swept extremes — a genuine three-way conflict, not
 * an artifact of one noisy fixture. No candidate in the swept range beats the pre-tuning baseline
 * on all three surfaces simultaneously, so nothing ships here either; RSI stays 25/75/v2. See
 * docs/CHANGELOG.md's E8-F1-S3 entry for the full figures. The E8-F4-S1 BUY-side mismatch is now
 * closed as a flagged finding: neither RSI bound, adjusted alone, fixes it — a resolution would
 * need a mechanism this pair of stories didn't test (e.g. per-asset thresholds, or accepting the
 * fixture-dependence as inherent to this indicator at a daily-candle horizon).
 *
 * <p><b>E8-F1-S4 pursued that per-asset-thresholds mechanism.</b> {@link PerSymbolRuleThresholds}
 * now resolves {@code rsiOverbought} per normalized ticker symbol, falling back to this class's
 * global {@link RuleThresholds#DEFAULT} (still 25/75) for every symbol without its own evidence.
 * {@code PerSymbolRsiOverboughtCalibrationTest} swept BTCUSDT/DOGEUSDT/SOLUSDT independently — each
 * against its own chronological 70% tuning window, each candidate then checked against that same
 * symbol's own 30% held-out tail (never another symbol's data, unlike E8-F4-S1's cross-symbol
 * check). Result: only SOLUSDT ships an override (70). BTCUSDT and DOGEUSDT's own tuning-window
 * winners (both 76) turned out to be indistinguishable from the current default (75) on those two
 * symbols' own held-out tails — candidates 71-76 produce byte-identical classification there, so
 * there was no held-out data capable of confirming or refuting the tuning-window gain, and an
 * unconfirmed candidate does not ship. {@link #RULE_TABLE_VERSION} bumps to v3 for the resolution
 * mechanism itself, per this story's confirmed scope, regardless of how many symbols ended up with
 * a non-default override. See {@link PerSymbolRuleThresholds}'s own class Javadoc and
 * docs/CHANGELOG.md's E8-F1-S4 entry for the full per-symbol sweep and figures.
 */
public final class SignalRuleEngine {

    public static final String RULE_TABLE_VERSION = "v3";

    public static final BigDecimal RSI_OVERSOLD_THRESHOLD = new BigDecimal("25");
    public static final BigDecimal RSI_OVERBOUGHT_THRESHOLD = new BigDecimal("75");
    public static final BigDecimal VOLATILITY_EXTREME_THRESHOLD = new BigDecimal("8.0");
    public static final BigDecimal VOLUME_DRIED_UP_THRESHOLD = new BigDecimal("0.20");

    /**
     * The four calibration-candidate thresholds bundled together (E8-F1-S1), so
     * {@code ThresholdCalibrationTest} can sweep candidate values through {@link #evaluate}
     * without reflectively mutating these production {@code static final} constants.
     * {@link #DEFAULT} is what every production caller uses via the 5-arg {@link #evaluate}
     * overload.
     */
    public record RuleThresholds(BigDecimal rsiOversold, BigDecimal rsiOverbought, BigDecimal volatilityExtreme,
                                  BigDecimal volumeDriedUp) {

        public static final RuleThresholds DEFAULT = new RuleThresholds(RSI_OVERSOLD_THRESHOLD,
                RSI_OVERBOUGHT_THRESHOLD, VOLATILITY_EXTREME_THRESHOLD, VOLUME_DRIED_UP_THRESHOLD);
    }

    /**
     * The three indicators' bullish/bearish reads (E8-F3-S1), extracted out of {@link #evaluate}
     * so both this class's own unweighted vote-counting and {@link WeightedVoteRuleEngine}'s
     * weighted vote share the exact same "what counts as a bullish/bearish read" logic — one
     * source of truth, not two copies that could quietly drift apart. Public (not just
     * package-private) because {@code BacktestHarness} (a different package, under
     * {@code com.autotrade.dashboard.backtest}) also needs it to score each indicator's own
     * directional read independently, per that story's AC.
     */
    public record IndicatorVotes(boolean rsiBullish, boolean rsiBearish, boolean macdBullish, boolean macdBearish,
                                  boolean maBullish, boolean maBearish) {
    }

    private SignalRuleEngine() {
    }

    /** Computes each indicator's bullish/bearish read against {@code thresholds}, with no gating
     * or vote-counting applied — a pure per-indicator classification. See {@link IndicatorVotes}. */
    public static IndicatorVotes computeVotes(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                               RuleThresholds thresholds) {
        boolean rsiBullish = rsi.compareTo(thresholds.rsiOversold()) < 0;
        boolean rsiBearish = rsi.compareTo(thresholds.rsiOverbought()) > 0;
        boolean macdBullish = macd.histogram().signum() > 0;
        boolean macdBearish = macd.histogram().signum() < 0;
        boolean maBullish = movingAverage.relation() == MovingAverageRelation.SHORT_ABOVE_LONG;
        boolean maBearish = movingAverage.relation() == MovingAverageRelation.SHORT_BELOW_LONG;
        return new IndicatorVotes(rsiBullish, rsiBearish, macdBullish, macdBearish, maBullish, maBearish);
    }

    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend) {
        return evaluate(rsi, macd, movingAverage, volatility, volumeTrend, RuleThresholds.DEFAULT);
    }

    public static SignalRuleId evaluate(BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                         BigDecimal volatility, BigDecimal volumeTrend, RuleThresholds thresholds) {
        if (volumeTrend == null) {
            return SignalRuleId.NO_VOLUME_DATA;
        }
        if (volumeTrend.compareTo(thresholds.volumeDriedUp()) < 0) {
            return SignalRuleId.VOLUME_DRIED_UP;
        }
        if (volatility.compareTo(thresholds.volatilityExtreme()) > 0) {
            return SignalRuleId.VOLATILITY_TOO_EXTREME;
        }

        IndicatorVotes votes = computeVotes(rsi, macd, movingAverage, thresholds);

        int bullishCount = count(votes.rsiBullish(), votes.macdBullish(), votes.maBullish());
        int bearishCount = count(votes.rsiBearish(), votes.macdBearish(), votes.maBearish());

        if (bullishCount > 0 && bearishCount > 0) {
            return SignalRuleId.CONFLICTING_SIGNALS;
        }
        if (bullishCount == 3) {
            return SignalRuleId.BULLISH_UNANIMOUS;
        }
        if (bullishCount == 2) {
            return SignalRuleId.BULLISH_MAJORITY;
        }
        if (bearishCount == 3) {
            return SignalRuleId.BEARISH_UNANIMOUS;
        }
        if (bearishCount == 2) {
            return SignalRuleId.BEARISH_MAJORITY;
        }
        return SignalRuleId.NO_STRONG_SIGNAL;
    }

    private static int count(boolean a, boolean b, boolean c) {
        return (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0);
    }
}
