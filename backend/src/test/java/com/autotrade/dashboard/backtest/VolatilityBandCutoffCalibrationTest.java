package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.HoldTermCalculator;
import com.autotrade.dashboard.signal.PerSymbolRuleThresholds;
import com.autotrade.dashboard.signal.RegimeClassifier;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.TrendStrength;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * E8-F6-S2: {@link HoldTermCalculator#VOLATILITY_LOW_MAX}/{@link
 * HoldTermCalculator#VOLATILITY_MEDIUM_MAX} (2.0/5.0) recalibration. E8-F6-S1 found {@link
 * com.autotrade.dashboard.signal.VolatilityBand#LOW} never occurs once across ~2,100 BUY/SELL
 * decision points on any of BTCUSDT/DOGEUSDT/SOLUSDT under the current 2.0 cutoff, permanently
 * killing {@code MODERATE_LOW} (and, independent of E8-F6-S3's unanimous-vote question, {@code
 * STRONG_LOW}) as dead branches. This test asks whether *any* cutoff could populate a genuinely
 * distinct LOW band for these crypto fixtures, not just relabel slices of the existing MEDIUM
 * band.
 *
 * <p><b>Mechanism</b>: reuses {@link FixtureSplits}'s chronological 70/30 tuning/held-out split
 * and the same production-gate-chain replay {@code HoldTermRangeCalibrationTest} established
 * (per-symbol thresholds -&gt; SELL regime gate -&gt; SELL MA-crossover gate, crypto-only,
 * matching {@code SignalService.computeSignalWithProvenance}), duplicated locally per this
 * repo's convention of not extracting a shared helper until a third use needs the identical
 * shape. Unlike that test, points here carry their raw ATR% {@code volatility} rather than a
 * pre-banded {@link com.autotrade.dashboard.signal.VolatilityBand} — banding is exactly what
 * this axis sweeps, so it can't be baked in at classification time. {@link TrendStrength} is
 * still derived up front (via the same {@link com.autotrade.dashboard.signal.SignalRuleId} switch
 * {@link HoldTermCalculator#calculate} uses) so the distinctness sweep can isolate {@code
 * MODERATE_LOW}/{@code MODERATE_MEDIUM} — the only branches this axis can plausibly populate,
 * since {@link TrendStrength#STRONG} (unanimous vote) essentially never fires regardless of
 * volatility (E8-F6-S1 finding, expected to hold here too and not chased as a bug).
 *
 * <p><b>Why static fields, not per-test recomputation</b>: {@code HoldTermRangeCalibrationTest}
 * reruns its production-gate replay independently inside each {@code @Test} method. This test
 * needs the *same* pooled-tuning ATR% distribution to both print the distribution report AND
 * derive the percentile-anchored candidate grid the sweep tests then reuse — computing it twice
 * would risk the grid silently drifting from the printed distribution it's supposed to be
 * anchored to. {@link #TUNING_POINTS_BY_SYMBOL}/{@link #HELD_OUT_POINTS_BY_SYMBOL} therefore
 * compute once, shared across every {@code @Test} method in this class.
 *
 * <p><b>Ship bar</b> (per E8-F6-S2's design gate, {@code MODERATE_LOW} only — the only branch
 * this axis can plausibly populate): (1) clears {@link #MIN_SCORED_FLOOR} (30, same floor E8-F6-S1
 * introduced) at its mid-day checkpoint (7 days, {@code MODERATE_LOW}'s already-shipped 3-10 day
 * range) on pooled tuning; (2) materially distinct {@link CheckpointStats#expectancyPctAfterCosts()}
 * vs. shrunk {@code MODERATE_MEDIUM} (at its own mid-day, 5 days, already-shipped 2-7 day range)
 * on pooled tuning, not noise-level; (3) the same distinctness replicates on pooled held-out; (4)
 * shrunk {@code MODERATE_MEDIUM} doesn't badly regress on any individual symbol's held-out curve
 * vs. its current (unshrunk, cutoff=2.0) held-out performance. {@code VOLATILITY_MEDIUM_MAX} stays
 * fixed at 5.0 throughout the primary sweep, only revisited if the LOW_MAX sweep produces a
 * promising candidate. No ship (every candidate fails the floor, or every floor-clearing
 * candidate turns out to be relabeling) is an equally valid, documented outcome, per every prior
 * E8 calibration story's precedent. Assertions here are structural only — the printed report is
 * the evidence under review; see docs/CHANGELOG.md's E8-F6-S2 entry for the recorded decision.
 *
 * <p><b>Out of scope</b>: {@code HoldTermRule}'s day ranges themselves (a separate axis, E8-F6-S1),
 * which branches exist (E8-F6-S3's call), and stock (AAPL) re-evaluation — crypto-only, mirroring
 * E8-F6-S1's own scope, per E8-F1-S7's precedent of keeping stock evaluation a separate follow-up.
 */
class VolatilityBandCutoffCalibrationTest {

    /** Same floor E8-F6-S1 introduced, re-declared locally rather than promoted to a shared
     * constant — same convention every prior E8 calibration test in this file family follows. */
    private static final int MIN_SCORED_FLOOR = 30;

    /** {@code MODERATE_LOW}'s already-shipped day range (3-10 days, mid 7) — day ranges
     * themselves are out of scope for this story, only used here as the fixed checkpoint a
     * candidate LOW band is scored at. */
    private static final int MODERATE_LOW_MIN_DAYS = 3;
    private static final int MODERATE_LOW_MID_DAYS = 7;
    private static final int MODERATE_LOW_MAX_DAYS = 10;

    /** {@code MODERATE_MEDIUM}'s already-shipped day range (2-7 days, mid 5). */
    private static final int MODERATE_MEDIUM_MIN_DAYS = 2;
    private static final int MODERATE_MEDIUM_MID_DAYS = 5;
    private static final int MODERATE_MEDIUM_MAX_DAYS = 7;

    /** Current production baseline, always included in the candidate grid for reference. */
    private static final BigDecimal BASELINE_LOW_MAX = HoldTermCalculator.VOLATILITY_LOW_MAX;
    private static final BigDecimal FIXED_MEDIUM_MAX = HoldTermCalculator.VOLATILITY_MEDIUM_MAX;

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    /** One BUY/SELL decision point carrying raw ATR% volatility (not pre-banded — banding is
     * what this axis sweeps) plus everything {@link WalkForwardScorer} needs, pre-sliced against
     * its own candle list. */
    private record VolPoint(TrendStrength trendStrength, BigDecimal volatility, BigDecimal decisionClose,
                             List<Candle> forward, boolean isBuy) {
    }

    private static final Map<String, List<VolPoint>> TUNING_POINTS_BY_SYMBOL = buildPointsBySymbol(true);
    private static final Map<String, List<VolPoint>> HELD_OUT_POINTS_BY_SYMBOL = buildPointsBySymbol(false);

    private static Map<String, List<VolPoint>> buildPointsBySymbol(boolean tuning) {
        Map<String, List<VolPoint>> result = new LinkedHashMap<>();
        for (SymbolFixture symbol : SYMBOLS) {
            List<Candle> candles = tuning ? symbol.tuning() : symbol.heldOut();
            String label = symbol.name() + (tuning ? " [tuning]" : " [held-out]");
            BacktestReport report = runProductionGates(symbol.name(), label, candles);
            assertStructurallySane(report);
            result.put(symbol.name(), classify(candles, report));
        }
        return result;
    }

    /** Replays the exact production gate chain {@code SignalService.computeSignalWithProvenance}
     * applies (per-symbol thresholds -&gt; SELL regime gate -&gt; SELL MA-crossover gate, both
     * crypto-only) — duplicated from {@code HoldTermRangeCalibrationTest}, per this repo's
     * convention of not extracting a shared helper until a third use needs the identical shape. */
    private static BacktestReport runProductionGates(String symbolName, String label, List<Candle> candles) {
        SignalRuleEngine.RuleThresholds thresholds = PerSymbolRuleThresholds.forSymbol(symbolName);
        BacktestHarness.RuleEvaluator evaluator = (rsi, macd, ma, volatility, volumeTrend) ->
                SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend, thresholds);
        return BacktestHarness.run(label, candles, evaluator, thresholds, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);
    }

    /** Derives {@link TrendStrength} via the same switch {@link HoldTermCalculator#calculate}
     * uses, but keeps raw {@code volatility} unbanded — see class Javadoc. */
    private static List<VolPoint> classify(List<Candle> candles, BacktestReport report) {
        List<VolPoint> result = new ArrayList<>();
        for (BacktestDecisionPoint point : report.buySellDecisionPoints()) {
            TrendStrength trendStrength = switch (point.matchedRule()) {
                case BULLISH_UNANIMOUS, BEARISH_UNANIMOUS -> TrendStrength.STRONG;
                case BULLISH_MAJORITY, BEARISH_MAJORITY -> TrendStrength.MODERATE;
                default -> throw new IllegalStateException(
                        "buySellDecisionPoints() must only contain BUY/SELL rules, got " + point.matchedRule());
            };
            BigDecimal decisionClose = candles.get(point.index()).close();
            List<Candle> forward = candles.subList(point.index() + 1, candles.size());
            boolean isBuy = point.matchedRule().call() == SignalCall.BUY;
            result.add(new VolPoint(trendStrength, point.volatility(), decisionClose, forward, isBuy));
        }
        return result;
    }

    private static void assertStructurallySane(BacktestReport report) {
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

    private static List<VolPoint> pooled(Map<String, List<VolPoint>> bySymbol) {
        List<VolPoint> pooled = new ArrayList<>();
        bySymbol.values().forEach(pooled::addAll);
        return pooled;
    }

    private static List<BigDecimal> sortedVolatilities(List<VolPoint> points) {
        List<BigDecimal> vols = new ArrayList<>(points.size());
        for (VolPoint p : points) {
            vols.add(p.volatility());
        }
        Collections.sort(vols);
        return vols;
    }

    /** Nearest-rank percentile over an already-ascending-sorted list — a diagnostic report, not a
     * statistical claim, so nearest-rank's simplicity over interpolation is an acceptable
     * tradeoff, consistent with every other E8 calibration test's "no p-values, eyeball the
     * printed curves" methodology. */
    private static BigDecimal percentile(List<BigDecimal> sortedAsc, int pct) {
        if (sortedAsc.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int n = sortedAsc.size();
        int idx = (int) Math.ceil(pct / 100.0 * n) - 1;
        idx = Math.max(0, Math.min(n - 1, idx));
        return sortedAsc.get(idx);
    }

    private static void printDistribution(String label, List<VolPoint> points) {
        List<BigDecimal> vols = sortedVolatilities(points);
        if (vols.isEmpty()) {
            System.out.printf("  %-24s n=0%n", label);
            return;
        }
        System.out.printf("  %-24s n=%-5d min=%6.2f%% p10=%6.2f%% p25=%6.2f%% p50=%6.2f%% p75=%6.2f%% p90=%6.2f%% max=%6.2f%%%n",
                label, vols.size(), vols.get(0), percentile(vols, 10), percentile(vols, 25),
                percentile(vols, 50), percentile(vols, 75), percentile(vols, 90), vols.get(vols.size() - 1));
    }

    @Test
    void reportsAtrPercentileDistribution() {
        System.out.println();
        System.out.println("########## E8-F6-S2: ATR% distribution at BUY/SELL decision points ##########");

        System.out.println();
        System.out.println("Tuning-window distributions (grid is anchored to these — never to held-out):");
        for (SymbolFixture symbol : SYMBOLS) {
            printDistribution(symbol.name() + " [tuning]", TUNING_POINTS_BY_SYMBOL.get(symbol.name()));
        }
        List<VolPoint> pooledTuning = pooled(TUNING_POINTS_BY_SYMBOL);
        printDistribution("POOLED [tuning]", pooledTuning);

        System.out.println();
        System.out.println("Held-out distributions (corroborating sanity check ONLY — never used to pick the candidate grid):");
        for (SymbolFixture symbol : SYMBOLS) {
            printDistribution(symbol.name() + " [held-out]", HELD_OUT_POINTS_BY_SYMBOL.get(symbol.name()));
        }
        printDistribution("POOLED [held-out]", pooled(HELD_OUT_POINTS_BY_SYMBOL));

        long strongCount = pooledTuning.stream().filter(p -> p.trendStrength() == TrendStrength.STRONG).count();
        System.out.println();
        System.out.printf("STRONG-trend pooled tuning points: n=%d (expected near-zero regardless of volatility band — "
                + "TrendStrength.STRONG essentially never fires, an E8-F6-S1 finding this story doesn't chase; "
                + "STRONG_LOW is expected to stay empty even if MODERATE_LOW gets populated, out of scope per E8-F6-S3).%n",
                strongCount);

        System.out.println();
        System.out.println("Percentile-anchored candidate VOLATILITY_LOW_MAX grid (from pooled tuning, rounded to 1dp):");
        for (BigDecimal candidate : candidateLowMaxGrid(pooledTuning)) {
            System.out.println("  " + candidate);
        }

        if (pooledTuning.isEmpty()) {
            throw new AssertionError("pooled tuning fixture must not be empty");
        }
    }

    /** Grid per design-gate: pooled tuning p5/p10/p20/p30/p40/p50 rounded to 1dp, plus the
     * current 2.0 baseline for reference — deduplicated and sorted ascending. */
    private static List<BigDecimal> candidateLowMaxGrid(List<VolPoint> pooledTuning) {
        List<BigDecimal> vols = sortedVolatilities(pooledTuning);
        TreeSet<BigDecimal> grid = new TreeSet<>();
        grid.add(BASELINE_LOW_MAX);
        for (int pct : List.of(5, 10, 20, 30, 40, 50)) {
            grid.add(percentile(vols, pct).setScale(1, RoundingMode.HALF_UP));
        }
        return new ArrayList<>(grid);
    }

    /** Win/loss/wash tally at a single fixed day, across every {@link VolPoint} in one candidate
     * band. Structurally the same small per-day accumulator shape {@code
     * HoldTermRangeCalibrationTest.DayAccumulator} uses — a distinct local copy, consistent with
     * this codebase's convention of one small accumulator per distinct scoring axis rather than a
     * shared abstraction extracted after only its second use. */
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

    private static CheckpointStats scoreAtDay(List<VolPoint> points, int day) {
        DayAccumulator acc = new DayAccumulator();
        for (VolPoint point : points) {
            Optional<WalkForwardScorer.CrossingEvent> crossing = WalkForwardScorer.findFirstCrossing(
                    point.forward(), day, point.decisionClose(), point.isBuy());
            Optional<DirectionalScoreResult> result = WalkForwardScorer.score(
                    point.forward(), day, point.decisionClose(), point.isBuy(), crossing);
            acc.record(result);
        }
        return acc.toStats();
    }

    private static void printCheckpoint(String label, int day, CheckpointStats stats) {
        String floorFlag = stats.scored() < MIN_SCORED_FLOOR ? " [below floor]" : "";
        System.out.printf("    %-28s d=%-3d %5.1f%%win exp%+7.3f%%(aft%+7.3f%%)(n=%-4d)%s%n",
                label, day, stats.winRate(), stats.expectancyPct(), stats.expectancyPctAfterCosts(), stats.scored(), floorFlag);
    }

    /** Filters {@code points} down to the {@link TrendStrength#MODERATE} population and partitions
     * by the candidate cutoffs: candidate LOW is {@code volatility < lowMax}, shrunk MEDIUM is
     * {@code [lowMax, mediumMax)}. */
    private static List<VolPoint>[] partitionModerate(List<VolPoint> points, BigDecimal lowMax, BigDecimal mediumMax) {
        List<VolPoint> low = new ArrayList<>();
        List<VolPoint> medium = new ArrayList<>();
        for (VolPoint point : points) {
            if (point.trendStrength() != TrendStrength.MODERATE) {
                continue;
            }
            if (point.volatility().compareTo(lowMax) < 0) {
                low.add(point);
            } else if (point.volatility().compareTo(mediumMax) < 0) {
                medium.add(point);
            }
        }
        @SuppressWarnings("unchecked")
        List<VolPoint>[] result = new List[] {low, medium};
        return result;
    }

    private static void sweepAndPrint(String context, List<VolPoint> points, List<BigDecimal> grid) {
        System.out.println();
        System.out.println("########## E8-F6-S2: candidate VOLATILITY_LOW_MAX sweep, " + context
                + " (MEDIUM_MAX fixed at " + FIXED_MEDIUM_MAX + ") ##########");
        for (BigDecimal candidate : grid) {
            if (candidate.compareTo(FIXED_MEDIUM_MAX) >= 0) {
                System.out.printf("%n  lowMax=%s: skipped (>= fixed mediumMax=%s, would invert the band)%n",
                        candidate, FIXED_MEDIUM_MAX);
                continue;
            }
            List<VolPoint>[] partitioned = partitionModerate(points, candidate, FIXED_MEDIUM_MAX);
            List<VolPoint> low = partitioned[0];
            List<VolPoint> medium = partitioned[1];
            String baselineTag = candidate.compareTo(BASELINE_LOW_MAX) == 0 ? " (current production baseline)" : "";
            System.out.printf("%n  lowMax=%s%s: MODERATE_LOW n=%d (raw), MODERATE_MEDIUM n=%d (raw, shrunk)%n",
                    candidate, baselineTag, low.size(), medium.size());

            printCheckpoint("MODERATE_LOW  (candidate)", MODERATE_LOW_MIN_DAYS, scoreAtDay(low, MODERATE_LOW_MIN_DAYS));
            printCheckpoint("MODERATE_LOW  (candidate)", MODERATE_LOW_MID_DAYS, scoreAtDay(low, MODERATE_LOW_MID_DAYS));
            printCheckpoint("MODERATE_LOW  (candidate)", MODERATE_LOW_MAX_DAYS, scoreAtDay(low, MODERATE_LOW_MAX_DAYS));
            printCheckpoint("MODERATE_MEDIUM (shrunk)", MODERATE_MEDIUM_MIN_DAYS, scoreAtDay(medium, MODERATE_MEDIUM_MIN_DAYS));
            printCheckpoint("MODERATE_MEDIUM (shrunk)", MODERATE_MEDIUM_MID_DAYS, scoreAtDay(medium, MODERATE_MEDIUM_MID_DAYS));
            printCheckpoint("MODERATE_MEDIUM (shrunk)", MODERATE_MEDIUM_MAX_DAYS, scoreAtDay(medium, MODERATE_MEDIUM_MAX_DAYS));
        }
    }

    @Test
    void sweepsCandidateLowMaxOnPooledTuningWindow() {
        List<VolPoint> pooledTuning = pooled(TUNING_POINTS_BY_SYMBOL);
        List<BigDecimal> grid = candidateLowMaxGrid(pooledTuning);
        sweepAndPrint("POOLED TUNING", pooledTuning, grid);

        if (pooledTuning.isEmpty()) {
            throw new AssertionError("pooled tuning fixture must not be empty");
        }
    }

    @Test
    void validatesPromisingCandidatesOnPooledHeldOutTail() {
        List<VolPoint> pooledTuning = pooled(TUNING_POINTS_BY_SYMBOL);
        List<BigDecimal> grid = candidateLowMaxGrid(pooledTuning);
        List<VolPoint> pooledHeldOut = pooled(HELD_OUT_POINTS_BY_SYMBOL);
        sweepAndPrint("POOLED HELD-OUT", pooledHeldOut, grid);

        if (pooledHeldOut.isEmpty()) {
            throw new AssertionError("pooled held-out fixture must not be empty");
        }
    }

    /** Per-symbol held-out regression check: does shrinking {@code MODERATE_MEDIUM} (by raising
     * the LOW cutoff) hurt any individual symbol's held-out {@code MODERATE_MEDIUM} performance
     * relative to today's unshrunk (baseline 2.0 cutoff) held-out performance? Uses the same grid
     * as the pooled sweeps, anchored to pooled tuning only. */
    @Test
    void checksModerateMediumRegressionOnEachSymbolHeldOutTail() {
        List<BigDecimal> grid = candidateLowMaxGrid(pooled(TUNING_POINTS_BY_SYMBOL));
        for (SymbolFixture symbol : SYMBOLS) {
            List<VolPoint> heldOut = HELD_OUT_POINTS_BY_SYMBOL.get(symbol.name());
            sweepAndPrint(symbol.name() + " HELD-OUT (individual-symbol MODERATE_MEDIUM regression check)", heldOut, grid);
        }

        for (SymbolFixture symbol : SYMBOLS) {
            if (HELD_OUT_POINTS_BY_SYMBOL.get(symbol.name()).isEmpty()) {
                throw new AssertionError(symbol.name() + " held-out fixture must not be empty");
            }
        }
    }
}
