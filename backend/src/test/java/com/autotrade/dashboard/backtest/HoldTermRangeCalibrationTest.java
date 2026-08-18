package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.HoldTermCalculator;
import com.autotrade.dashboard.signal.HoldTermRule;
import com.autotrade.dashboard.signal.PerSymbolRuleThresholds;
import com.autotrade.dashboard.signal.RegimeClassifier;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.TrendStrength;
import com.autotrade.dashboard.signal.VolatilityBand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * E8-F6-S1: {@link HoldTermRule}'s 6-branch day-range table has carried the same doc-comment
 * flag since E2-F3-S2 shipped it — "provisional engineering estimates, not yet backtest-validated
 * — revisit once E2-F4-S1's backtest harness exists and can check realized hold-term accuracy,
 * not just call win/loss." That harness has existed since E2-F4-S1 and grew TP/SL-aware scoring
 * (E8-F2-S1), transaction costs (E8-F2-S2), and per-checkpoint holding-duration tracking
 * (E8-F2-S3) — {@link BacktestHarness#run} already scores every BUY/SELL call at exactly its
 * matched branch's MIN/MID/MAX day count — but nothing has ever swept alternative day counts
 * against a branch's own decision-point population to check whether its provisional range is
 * actually good. This is that check.
 *
 * <p><b>Mechanism</b>: no changes to {@link BacktestHarness}/{@link HoldTermCalculator}/
 * {@link HoldTermRule} were needed. {@link BacktestReport#buySellDecisionPoints()} already carries
 * everything needed to re-classify and re-score every historical BUY/SELL decision point: {@link
 * BacktestDecisionPoint#matchedRule()} gives {@link TrendStrength} via the exact same 4-way switch
 * {@link HoldTermCalculator#calculate} uses, {@link BacktestDecisionPoint#volatility()} gives
 * {@link VolatilityBand} via {@link HoldTermCalculator}'s own public
 * {@code VOLATILITY_LOW_MAX}/{@code VOLATILITY_MEDIUM_MAX} constants, and {@link
 * HoldTermRule#match} (already public) resolves the real branch — the actual production matcher,
 * not a reimplementation. {@link BacktestDecisionPoint#index()} lets a candidate day's decision
 * close/forward candles be reconstructed from the same candle list {@link BacktestHarness#run} was
 * given. Each decision point is replayed through {@link SignalService}'s exact production gate
 * chain (per-symbol thresholds → SELL regime gate → SELL MA-crossover gate, all crypto-only,
 * matching {@code SignalService.computeSignalWithProvenance}) so the classified population matches
 * what real hold-term suggestions are generated for today.
 *
 * <p><b>Classification gotcha</b>: {@code STRONG_MEDIUM} (3-10 days) and {@code MODERATE_LOW}
 * (3-10 days) currently have byte-identical {@code (minDays, maxDays)} — classifying from {@link
 * com.autotrade.dashboard.signal.HoldTerm#label()} alone would be ambiguous. Classification always
 * goes through {@link TrendStrength}+{@link VolatilityBand}, never through the derived
 * {@code HoldTerm} value.
 *
 * <p><b>Metric</b>: {@link CheckpointStats#expectancyPctAfterCosts()}, not win rate alone and not
 * {@link CheckpointStats#expectancyPctAfterCostsAndFunding()} — every prior E8-F1/F3 calibration
 * story's ship bar is stated this way, and funding cost is Binance-Futures-specific while {@link
 * HoldTermRule}/{@link HoldTermCalculator} carry no asset-type parameter and are meant to
 * eventually generalize to stock hold-term suggestions too; gating a signal-quality decision on a
 * broker-mechanics cost that doesn't apply to every asset type this table serves would be wrong on
 * the merits, not just inconsistent with precedent. Funding-adjusted figures aren't printed here
 * (unlike other calibration tests) since they'd be misleading noise for AAPL-eventual generality,
 * not decision-relevant context.
 *
 * <p><b>Sweep grain</b>: single fixed-day horizons over a 13-point grid ({@link #CANDIDATE_DAYS})
 * spanning below the narrowest current branch (1 day) through beyond the widest (25 days) — finer
 * than the 6 branches' current 3-point MIN/MID/MAX checkpoints, so the sweep doesn't inherit an
 * assumption about where the "real" range should sit. A branch's current min/mid/max are
 * themselves three points already inside this same curve (mid = {@code round((min+max)/2.0)},
 * matching {@link BacktestHarness#run}'s own rounding), so "does a candidate range beat the
 * current one" reduces to comparing curve values at candidate days vs. the branch's own current
 * days — no separate baseline run needed. Per {@link WalkForwardScorer#findFirstCrossing}'s own
 * contract ("Runs once per decision point and is then applied to each Checkpoint independently by
 * score, bounded by that checkpoint's own day count"), each decision point's TP/SL crossing scan
 * runs once at the grid's max day and is reused across every candidate day in the grid, not
 * rescanned per candidate.
 *
 * <p><b>Validation methodology</b>: pooled global first, not per-symbol from the outset — unlike
 * {@code RuleThresholds}' raw price-behavior thresholds (RSI/MACD/MA), which needed per-symbol
 * forks (E8-F1-S4/S8/S10) only after their *global* sweeps showed asset-divergent optima, {@link
 * HoldTermRule} branches are keyed by {@link TrendStrength}x{@link VolatilityBand} — both already
 * ATR%-normalized/agreement-derived quantities. This is the first look at hold-term ranges at all,
 * so it follows E8-F1-S1's own order-of-operations: try pooled first, only escalate to a
 * per-branch-per-symbol mechanism in a follow-up story if the pooled sweep's per-symbol breakdown
 * shows the same divergence pattern RSI hit. Reuses {@link FixtureSplits}'s existing chronological
 * 70/30 split verbatim — no new fixture, no new split logic.
 *
 * <p><b>Ship bar</b> (per branch, independently): a candidate range ships only if it beats the
 * branch's current range on {@code expectancyPctAfterCosts()} on the <b>pooled</b> tuning curve
 * with adequate {@code n}, <i>and</i> still beats it on the <b>pooled</b> held-out curve, <i>and</i>
 * doesn't regress badly on any <b>individual</b> symbol's held-out curve. A branch that fails any
 * leg keeps its current provisional range — a legitimate, documented per-branch no-ship, same
 * granularity precedent as E8-F1-S4/S8/S10's per-symbol no-ships, applied per-branch here instead.
 * {@link #MIN_SCORED_FLOOR} (30, a new floor this story introduces — no prior E8 test hardcodes a
 * sample-size number) gates candidate eligibility: this axis partitions an already BUY/SELL-only
 * population <i>jointly</i> by TrendStrength AND VolatilityBand at once (six ways), a structurally
 * worse sparsity risk than any single-axis E8-F1 threshold sweep. A branch that never clears the
 * floor near its current range at every symbol is recorded as "insufficient data to calibrate" —
 * current range left unchanged, not silently reported as validated, mirroring E8-F3-S4's
 * distinction between "asset-dependent, no confirmable winner" and a clean no-ship.
 *
 * <p><b>Versioning</b>: {@link HoldTermCalculator#HOLD_TERM_TABLE_VERSION} bumps only if at least
 * one branch's {@code minDays}/{@code maxDays} literal actually changes; {@link
 * com.autotrade.dashboard.signal.SignalRuleEngine#RULE_TABLE_VERSION} is untouched (this story
 * never changes which {@link SignalRuleId} a decision point resolves to).
 *
 * <p><b>Out of scope</b>: which {@link TrendStrength}/{@link VolatilityBand} pairs exist, the
 * volatility-band threshold constants themselves, any {@link com.autotrade.dashboard.signal.SignalRuleEngine}
 * change, per-symbol hold-term overrides, AAPL evaluation, and any live-monitoring hold-term-drift
 * hook — all explicit possible follow-ups, not folded in here.
 *
 * <p>No ship at all (every branch's current range confirmed, or recorded insufficient-data) is an
 * equally valid, documented outcome, per every prior E8 calibration story's own precedent.
 * Assertions here are structural only — the printed report is the evidence under review. Read the
 * printed output (rerun via {@code ./mvnw test -Dtest=HoldTermRangeCalibrationTest}) for the actual
 * figures; see docs/CHANGELOG.md's E8-F6-S1 entry for the recorded decision.
 */
class HoldTermRangeCalibrationTest {

    /** Spans below the narrowest current branch (MODERATE_HIGH, 1-4 days) through beyond the
     * widest (STRONG_LOW, 5-15 days) — see class Javadoc's "Sweep grain" section. */
    private static final List<Integer> CANDIDATE_DAYS = List.of(1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 15, 20, 25);
    private static final int MAX_CANDIDATE_DAY = CANDIDATE_DAYS.get(CANDIDATE_DAYS.size() - 1);

    /** New floor this story introduces — see class Javadoc's "Ship bar" section for why this axis
     * needs one where no prior E8 calibration test did. */
    private static final int MIN_SCORED_FLOOR = 30;

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    /** One classified decision point, pre-sliced against its own candle list so pooling points
     * from different symbols into one sweep needs no further bookkeeping. */
    private record ClassifiedPoint(HoldTermRule branch, BigDecimal decisionClose, List<Candle> forward, boolean isBuy) {
    }

    @Test
    void sweepPooledTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F6-S1: HoldTermRule day-range sweep, POOLED TUNING WINDOW "
                + "(BTCUSDT+DOGEUSDT+SOLUSDT, first " + FixtureSplits.SPLIT_INDEX + " candles each) ##########");

        List<ClassifiedPoint> pooled = new ArrayList<>();
        for (SymbolFixture symbol : SYMBOLS) {
            BacktestReport report = runProductionGates(symbol.name(), symbol.name() + " [tuning]", symbol.tuning());
            assertStructurallySane(report);
            pooled.addAll(classify(symbol.tuning(), report.buySellDecisionPoints()));
        }

        printCurves("pooled tuning", sweep(pooled));
    }

    @Test
    void validatePooledHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F6-S1: HoldTermRule day-range sweep, POOLED HELD-OUT TAIL "
                + "(BTCUSDT+DOGEUSDT+SOLUSDT, candles " + FixtureSplits.SPLIT_INDEX + "-1000) ##########");

        List<ClassifiedPoint> pooled = new ArrayList<>();
        for (SymbolFixture symbol : SYMBOLS) {
            BacktestReport report = runProductionGates(symbol.name(), symbol.name() + " [held-out]", symbol.heldOut());
            assertStructurallySane(report);
            pooled.addAll(classify(symbol.heldOut(), report.buySellDecisionPoints()));
        }

        printCurves("pooled held-out", sweep(pooled));
    }

    @Test
    void validateEachSymbolOwnHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F6-S1: HoldTermRule day-range sweep, EACH SYMBOL'S OWN HELD-OUT TAIL "
                + "(individual-symbol regression check) ##########");

        for (SymbolFixture symbol : SYMBOLS) {
            BacktestReport report = runProductionGates(symbol.name(), symbol.name() + " [held-out]", symbol.heldOut());
            assertStructurallySane(report);
            List<ClassifiedPoint> points = classify(symbol.heldOut(), report.buySellDecisionPoints());
            printCurves(symbol.name() + " held-out", sweep(points));
        }
    }

    /** Replays the exact production gate chain {@code SignalService.computeSignalWithProvenance}
     * applies (per-symbol thresholds -&gt; SELL regime gate -&gt; SELL MA-crossover gate, both
     * crypto-only) so the decision-point population feeding this calibration matches what real
     * hold-term suggestions are generated for today, not a bare-default replay. */
    private static BacktestReport runProductionGates(String symbolName, String label, List<Candle> candles) {
        SignalRuleEngine.RuleThresholds thresholds = PerSymbolRuleThresholds.forSymbol(symbolName);
        BacktestHarness.RuleEvaluator evaluator = (rsi, macd, ma, volatility, volumeTrend) ->
                SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend, thresholds);
        return BacktestHarness.run(label, candles, evaluator, thresholds, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);
    }

    /** Classifies every BUY/SELL decision point into its {@link HoldTermRule} branch using the
     * exact same {@link TrendStrength}/{@link VolatilityBand} derivation {@link
     * HoldTermCalculator#calculate} uses in production, and pre-slices decision-close/forward
     * candles from {@code candles} so downstream sweeping needs no further reference to the
     * original list. */
    private static List<ClassifiedPoint> classify(List<Candle> candles, List<BacktestDecisionPoint> points) {
        List<ClassifiedPoint> result = new ArrayList<>();
        for (BacktestDecisionPoint point : points) {
            TrendStrength trendStrength = switch (point.matchedRule()) {
                case BULLISH_UNANIMOUS, BEARISH_UNANIMOUS -> TrendStrength.STRONG;
                case BULLISH_MAJORITY, BEARISH_MAJORITY -> TrendStrength.MODERATE;
                default -> throw new IllegalStateException(
                        "buySellDecisionPoints() must only contain BUY/SELL rules, got " + point.matchedRule());
            };
            VolatilityBand volatilityBand = classifyVolatility(point.volatility());
            HoldTermRule branch = HoldTermRule.match(trendStrength, volatilityBand);

            BigDecimal decisionClose = candles.get(point.index()).close();
            List<Candle> forward = candles.subList(point.index() + 1, candles.size());
            boolean isBuy = point.matchedRule().call() == SignalCall.BUY;
            result.add(new ClassifiedPoint(branch, decisionClose, forward, isBuy));
        }
        return result;
    }

    /** Mirrors {@link HoldTermCalculator}'s own private {@code classifyVolatility} exactly, using
     * its public {@code VOLATILITY_LOW_MAX}/{@code VOLATILITY_MEDIUM_MAX} constants — the real
     * production banding, not a reimplementation with independently-chosen cutoffs. */
    private static VolatilityBand classifyVolatility(BigDecimal volatility) {
        if (volatility.compareTo(HoldTermCalculator.VOLATILITY_LOW_MAX) < 0) {
            return VolatilityBand.LOW;
        }
        if (volatility.compareTo(HoldTermCalculator.VOLATILITY_MEDIUM_MAX) < 0) {
            return VolatilityBand.MEDIUM;
        }
        return VolatilityBand.HIGH;
    }

    /** Sweeps {@link #CANDIDATE_DAYS} against every branch's own classified points. Each point's
     * TP/SL crossing scan runs once at {@link #MAX_CANDIDATE_DAY} and is reused across every
     * candidate day, per {@link WalkForwardScorer#findFirstCrossing}'s own "runs once, applied per
     * checkpoint" contract. */
    private static Map<HoldTermRule, Map<Integer, CheckpointStats>> sweep(List<ClassifiedPoint> points) {
        Map<HoldTermRule, Map<Integer, DayAccumulator>> acc = new EnumMap<>(HoldTermRule.class);
        for (HoldTermRule rule : HoldTermRule.values()) {
            Map<Integer, DayAccumulator> perDay = new LinkedHashMap<>();
            for (int day : CANDIDATE_DAYS) {
                perDay.put(day, new DayAccumulator());
            }
            acc.put(rule, perDay);
        }

        for (ClassifiedPoint point : points) {
            Optional<WalkForwardScorer.CrossingEvent> crossing = WalkForwardScorer.findFirstCrossing(
                    point.forward(), MAX_CANDIDATE_DAY, point.decisionClose(), point.isBuy());
            Map<Integer, DayAccumulator> perDay = acc.get(point.branch());
            for (int day : CANDIDATE_DAYS) {
                Optional<DirectionalScoreResult> result = WalkForwardScorer.score(
                        point.forward(), day, point.decisionClose(), point.isBuy(), crossing);
                perDay.get(day).record(result);
            }
        }

        Map<HoldTermRule, Map<Integer, CheckpointStats>> stats = new EnumMap<>(HoldTermRule.class);
        acc.forEach((rule, perDay) -> {
            Map<Integer, CheckpointStats> dayStats = new LinkedHashMap<>();
            perDay.forEach((day, dayAcc) -> dayStats.put(day, dayAcc.toStats()));
            stats.put(rule, dayStats);
        });
        return stats;
    }

    private void printCurves(String context, Map<HoldTermRule, Map<Integer, CheckpointStats>> curves) {
        for (HoldTermRule rule : HoldTermRule.values()) {
            printBranchCurve(context, rule, curves.get(rule));
        }
    }

    private void printBranchCurve(String context, HoldTermRule rule, Map<Integer, CheckpointStats> curve) {
        int midDays = (int) Math.round((rule.minDays() + rule.maxDays()) / 2.0);
        System.out.printf("%n  %s [%s] (current %d-%d days, mid %d):%n", rule, context, rule.minDays(), rule.maxDays(), midDays);
        for (int day : CANDIDATE_DAYS) {
            CheckpointStats stats = curve.get(day);
            String marker = day == rule.minDays() ? "  <- current min"
                    : day == midDays ? "  <- current mid"
                    : day == rule.maxDays() ? "  <- current max" : "";
            if (stats.scored() == 0) {
                System.out.printf("    d=%-3d (n=0)%s%n", day, marker);
                continue;
            }
            String floorFlag = stats.scored() < MIN_SCORED_FLOOR ? " [below floor]" : "";
            System.out.printf("    d=%-3d %5.1f%%win exp%+7.3f%%(aft%+7.3f%%)(n=%-4d)%s%s%n",
                    day, stats.winRate(), stats.expectancyPct(), stats.expectancyPctAfterCosts(), stats.scored(),
                    floorFlag, marker);
        }
    }

    /** Same structural invariant every other E8 calibration test checks: every classified
     * decision point must have landed in exactly one BUY/SELL {@link SignalRuleId} bucket (the
     * gate chain never demotes a call to HOLD without also removing its {@code holdTerm}, which is
     * what keeps it out of {@code buySellDecisionPoints()} in the first place). */
    private void assertStructurallySane(BacktestReport report) {
        int totalFromCounts = report.callCounts().values().stream().mapToInt(Integer::intValue).sum();
        if (report.totalDecisionPoints() != totalFromCounts) {
            throw new AssertionError(report.label() + ": every decision point must land in exactly one SignalRuleId bucket");
        }
        for (BacktestDecisionPoint point : report.buySellDecisionPoints()) {
            SignalCall call = point.matchedRule().call();
            if (call != SignalCall.BUY && call != SignalCall.SELL) {
                throw new AssertionError(report.label() + ": buySellDecisionPoints() contained a non-BUY/SELL rule "
                        + point.matchedRule());
            }
        }
    }

    /** Win/loss/wash tally at a single candidate day, across every classified decision point for
     * one {@link HoldTermRule} branch — structurally the same small accumulator shape as {@code
     * BacktestHarness}'s private {@code IndicatorAccumulator}/{@code HoldGateAccumulator}, which
     * aren't reusable across files (package-private nesting); a fourth small local accumulator is
     * consistent with this codebase's existing convention of one per distinct scoring axis, not a
     * DRY violation worth avoiding. */
    private static final class DayAccumulator {
        int win;
        int loss;
        int wash;
        int notScored;
        int tpHit;
        int slHit;
        int horizonExpired;
        BigDecimal winReturnSum = BigDecimal.ZERO;
        BigDecimal lossReturnSum = BigDecimal.ZERO;
        long holdingDaysSum;

        void record(Optional<DirectionalScoreResult> result) {
            if (result.isEmpty()) {
                notScored++;
                return;
            }
            DirectionalScoreResult r = result.get();
            holdingDaysSum += r.daysHeld();
            switch (r.exitReason()) {
                case TP_HIT -> tpHit++;
                case SL_HIT -> slHit++;
                case HORIZON_EXPIRED -> horizonExpired++;
            }
            switch (r.outcome()) {
                case WIN -> {
                    win++;
                    winReturnSum = winReturnSum.add(r.signedReturnPct());
                }
                case LOSS -> {
                    loss++;
                    lossReturnSum = lossReturnSum.add(r.signedReturnPct());
                }
                case WASH -> wash++;
            }
        }

        CheckpointStats toStats() {
            int scored = win + loss + wash;
            double avgWin = win == 0 ? 0.0 : winReturnSum.doubleValue() / win;
            double avgLoss = loss == 0 ? 0.0 : lossReturnSum.doubleValue() / loss;
            double avgHoldingDays = scored == 0 ? 0.0 : holdingDaysSum / (double) scored;
            return new CheckpointStats(win, loss, wash, notScored, avgWin, avgLoss, tpHit, slHit, horizonExpired, avgHoldingDays);
        }
    }
}
