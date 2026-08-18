package com.autotrade.dashboard.backtest;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.signal.PerSymbolRuleThresholds;
import com.autotrade.dashboard.signal.RegimeClassifier;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalRuleEngine;
import com.autotrade.dashboard.signal.SignalRuleEngine.IndicatorVotes;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.TrendStrength;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * E8-F6-S3: is {@link TrendStrength#STRONG} (unanimous RSI+MACD+MA-crossover agreement, feeding
 * {@code HoldTermRule}'s {@code STRONG_*} branches) a real-but-rare regime, or effectively
 * unreachable under the currently-calibrated per-axis thresholds? E8-F6-S1 found zero
 * STRONG-classified decision points across ~2,100 real crypto BUY/SELL calls even though {@link
 * SignalRuleEngine#evaluate}'s {@code bullishCount == 3}/{@code bearishCount == 3} branch is
 * reachable in principle, not gated out structurally — this story measures, rather than assumes,
 * why.
 *
 * <p><b>Mechanism</b>: {@link BacktestDecisionPoint} now carries the {@link IndicatorVotes}
 * {@code BacktestHarness#run}'s own loop already computes per decision point (E8-F6-S3 threaded
 * this through — see that record's own Javadoc), so this test doesn't need to recompute
 * indicators from the candle window a second time. For every {@code BULLISH_MAJORITY}/{@code
 * BEARISH_MAJORITY}/{@code BULLISH_UNANIMOUS}/{@code BEARISH_UNANIMOUS} decision point (the only
 * rules {@link com.autotrade.dashboard.signal.HoldTermCalculator#calculate} ever sees), {@link
 * DissentTally} re-derives per-axis agree/oppose from the stored votes and identifies which single
 * indicator (if any) stayed neutral rather than joining the other two — the "how close to
 * unanimity" measurement the story's AC calls for. A MAJORITY call structurally has zero opposing
 * votes (an opposing vote alongside two agreeing ones resolves to {@code CONFLICTING_SIGNALS} in
 * {@link SignalRuleEngine#evaluate}, never MAJORITY) so the third indicator is always neutral, not
 * dissenting in the sense of actively disagreeing — {@link DissentTally#record} asserts this
 * invariant rather than assuming it.
 *
 * <p><b>Primary population</b>: the real production gate chain (per-symbol thresholds -&gt; SELL
 * regime gate -&gt; SELL MA-crossover gate, crypto-only), duplicated locally per this repo's
 * convention of not extracting a shared helper until a third use needs the identical shape,
 * matching every other E8-F6 story's population — this is what real hold-term suggestions are
 * generated against today.
 *
 * <p><b>Secondary comparison</b>: the AC specifically asks whether an already-shipped per-axis
 * calibration ({@link PerSymbolRuleThresholds}'s SOLUSDT-only {@code rsiOverbought=70}/{@code
 * macdMinHistogramMagnitudePct=0.10} overrides, E8-F1-S4/E8-F1-S8) is the structural reason
 * unanimity is unreachable, distinct from "genuinely rare regardless of tuning." {@link
 * #comparesDissentPatternUnderDefaultThresholds()} re-runs the bare rule table (no SELL gates —
 * those only ever remove points from the population post-hoc, they never change which indicator
 * dissents on a vote, so including them here would add noise without isolating the threshold
 * axis) under both {@link PerSymbolRuleThresholds#forSymbol} and the global {@link
 * RuleThresholds#DEFAULT}. BTCUSDT/DOGEUSDT have no override at all, so their two figures must
 * match exactly — a built-in control confirming the comparison mechanism itself is sound — while
 * SOLUSDT's two figures diverging (or not) is the actual evidence for or against the hypothesis.
 *
 * <p><b>Per this story's confirmed scope</b>: a documented decision, not a code requirement.
 * Touching an already-shipped per-axis calibration to chase this finding is explicitly out of
 * scope (per-axis thresholds stay whatever E8-F1-S4/S8/S10/S12 left them at); this test only
 * decides whether the structural reason traces to that calibration or not. If it doesn't,
 * {@code HoldTermRule.STRONG_*} branches are recorded as rare-but-real (dead in these fixtures,
 * not necessarily for an untested asset class per E8-F1-S7's stock-divergence precedent) rather
 * than removed. See docs/CHANGELOG.md's E8-F6-S3 entry for the recorded decision and the actual
 * printed figures.
 */
class UnanimityGapAnalysisTest {

    private record SymbolFixture(String name, List<Candle> tuning, List<Candle> heldOut) {
    }

    private static final List<SymbolFixture> SYMBOLS = List.of(
            new SymbolFixture("BTCUSDT", FixtureSplits.BTCUSDT_TUNING, FixtureSplits.BTCUSDT_HELD_OUT),
            new SymbolFixture("DOGEUSDT", FixtureSplits.DOGEUSDT_TUNING, FixtureSplits.DOGEUSDT_HELD_OUT),
            new SymbolFixture("SOLUSDT", FixtureSplits.SOLUSDT_TUNING, FixtureSplits.SOLUSDT_HELD_OUT));

    /** Replays the exact production gate chain {@code SignalService.computeSignalWithProvenance}
     * applies (per-symbol thresholds -&gt; SELL regime gate -&gt; SELL MA-crossover gate, both
     * crypto-only) — duplicated from {@code HoldTermRangeCalibrationTest}/{@code
     * VolatilityBandCutoffCalibrationTest}, per this repo's convention of not extracting a shared
     * helper until a third use needs the identical shape. */
    private static BacktestReport runProductionGates(String symbolName, String label, List<Candle> candles) {
        RuleThresholds thresholds = PerSymbolRuleThresholds.forSymbol(symbolName);
        BacktestHarness.RuleEvaluator evaluator = (rsi, macd, ma, volatility, volumeTrend) ->
                SignalRuleEngine.evaluate(rsi, macd, ma, volatility, volumeTrend, thresholds);
        return BacktestHarness.run(label, candles, evaluator, thresholds, true, RegimeClassifier.ADX_TRENDING_THRESHOLD, true);
    }

    /** Bare rule table under a given {@code thresholds}, no SELL gates — isolates just the
     * RSI/MACD/MA vote-threshold axis for {@link #comparesDissentPatternUnderDefaultThresholds()}. */
    private static BacktestReport runBareRuleTable(String label, List<Candle> candles, RuleThresholds thresholds) {
        return BacktestHarness.run(label, candles, thresholds);
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

    /** Tallies, across a population of BUY/SELL decision points, which single indicator stayed
     * neutral (kept a MAJORITY call from being UNANIMOUS) — split by direction, since BUY/SELL
     * have shown asymmetric behavior on every other E8 threshold axis. */
    private static final class DissentTally {
        int unanimous;
        int rsiNeutral;
        int macdNeutral;
        int maNeutral;
        int total;

        void record(SignalRuleId matchedRule, IndicatorVotes votes) {
            boolean isBuy = matchedRule.call() == SignalCall.BUY;
            boolean isSell = matchedRule.call() == SignalCall.SELL;
            if (!isBuy && !isSell) {
                throw new IllegalStateException("expected only BUY/SELL rules here, got " + matchedRule);
            }
            boolean rsiAgrees = isBuy ? votes.rsiBullish() : votes.rsiBearish();
            boolean macdAgrees = isBuy ? votes.macdBullish() : votes.macdBearish();
            boolean maAgrees = isBuy ? votes.maBullish() : votes.maBearish();
            boolean rsiOpposes = isBuy ? votes.rsiBearish() : votes.rsiBullish();
            boolean macdOpposes = isBuy ? votes.macdBearish() : votes.macdBullish();
            boolean maOpposes = isBuy ? votes.maBearish() : votes.maBullish();
            if (rsiOpposes || macdOpposes || maOpposes) {
                throw new IllegalStateException(matchedRule + " decision point has an opposing vote — "
                        + "MAJORITY/UNANIMOUS should be unreachable with any dissent present (would be CONFLICTING_SIGNALS)");
            }

            total++;
            int agreeCount = (rsiAgrees ? 1 : 0) + (macdAgrees ? 1 : 0) + (maAgrees ? 1 : 0);
            if (agreeCount == 3) {
                unanimous++;
            } else if (agreeCount == 2) {
                if (!rsiAgrees) {
                    rsiNeutral++;
                } else if (!macdAgrees) {
                    macdNeutral++;
                } else {
                    maNeutral++;
                }
            } else {
                throw new IllegalStateException(matchedRule + " MAJORITY/UNANIMOUS decision point resolved with only "
                        + agreeCount + " agreeing votes");
            }
        }
    }

    private record DirectionalTally(DissentTally buy, DissentTally sell) {
        static DirectionalTally of(List<BacktestDecisionPoint> points) {
            DissentTally buy = new DissentTally();
            DissentTally sell = new DissentTally();
            for (BacktestDecisionPoint point : points) {
                DissentTally tally = point.matchedRule().call() == SignalCall.BUY ? buy : sell;
                tally.record(point.matchedRule(), point.votes());
            }
            return new DirectionalTally(buy, sell);
        }
    }

    private static double pct(int part, int total) {
        return total == 0 ? 0.0 : 100.0 * part / total;
    }

    private static void printTally(String label, DissentTally t) {
        if (t.total == 0) {
            System.out.printf("    %-24s n=0%n", label);
            return;
        }
        System.out.printf("    %-24s n=%-5d unanimous=%-4d(%5.1f%%) rsiNeutral=%-4d(%5.1f%%) macdNeutral=%-4d(%5.1f%%) maNeutral=%-4d(%5.1f%%)%n",
                label, t.total, t.unanimous, pct(t.unanimous, t.total),
                t.rsiNeutral, pct(t.rsiNeutral, t.total),
                t.macdNeutral, pct(t.macdNeutral, t.total),
                t.maNeutral, pct(t.maNeutral, t.total));
    }

    private static void printDirectional(String context, DirectionalTally tally) {
        System.out.println("  " + context + ":");
        printTally("BUY", tally.buy());
        printTally("SELL", tally.sell());
    }

    @Test
    void reportsUnanimityGapOnPooledTuningWindow() {
        System.out.println();
        System.out.println("########## E8-F6-S3: unanimity-gap analysis, POOLED TUNING WINDOW ##########");

        DissentTally pooledBuy = new DissentTally();
        DissentTally pooledSell = new DissentTally();
        for (SymbolFixture symbol : SYMBOLS) {
            BacktestReport report = runProductionGates(symbol.name(), symbol.name() + " [tuning]", symbol.tuning());
            assertStructurallySane(report);
            DirectionalTally tally = DirectionalTally.of(report.buySellDecisionPoints());
            printDirectional(symbol.name(), tally);
            for (BacktestDecisionPoint point : report.buySellDecisionPoints()) {
                DissentTally pooled = point.matchedRule().call() == SignalCall.BUY ? pooledBuy : pooledSell;
                pooled.record(point.matchedRule(), point.votes());
            }
        }
        System.out.println();
        printDirectional("POOLED", new DirectionalTally(pooledBuy, pooledSell));

        if (pooledBuy.total == 0 && pooledSell.total == 0) {
            throw new AssertionError("pooled tuning window must produce at least one BUY/SELL decision point");
        }
    }

    @Test
    void reportsUnanimityGapOnPooledHeldOutTail() {
        System.out.println();
        System.out.println("########## E8-F6-S3: unanimity-gap analysis, POOLED HELD-OUT TAIL ##########");

        DissentTally pooledBuy = new DissentTally();
        DissentTally pooledSell = new DissentTally();
        for (SymbolFixture symbol : SYMBOLS) {
            BacktestReport report = runProductionGates(symbol.name(), symbol.name() + " [held-out]", symbol.heldOut());
            assertStructurallySane(report);
            DirectionalTally tally = DirectionalTally.of(report.buySellDecisionPoints());
            printDirectional(symbol.name(), tally);
            for (BacktestDecisionPoint point : report.buySellDecisionPoints()) {
                DissentTally pooled = point.matchedRule().call() == SignalCall.BUY ? pooledBuy : pooledSell;
                pooled.record(point.matchedRule(), point.votes());
            }
        }
        System.out.println();
        printDirectional("POOLED", new DirectionalTally(pooledBuy, pooledSell));

        if (pooledBuy.total == 0 && pooledSell.total == 0) {
            throw new AssertionError("pooled held-out tail must produce at least one BUY/SELL decision point");
        }
    }

    /** Isolates the {@link PerSymbolRuleThresholds} hypothesis: does SOLUSDT's shipped
     * {@code rsiOverbought=70}/{@code macdMinHistogramMagnitudePct=0.10} override change its
     * unanimity rate versus the global default, while BTCUSDT/DOGEUSDT (no override, so both runs
     * must be byte-identical for them) hold still as a control? */
    @Test
    void comparesDissentPatternUnderDefaultThresholds() {
        System.out.println();
        System.out.println("########## E8-F6-S3: dissent pattern, calibrated per-symbol thresholds vs. RuleThresholds.DEFAULT ##########");
        System.out.println("(no SELL gates applied — isolates the RSI/MACD/MA vote-threshold axis only; BTCUSDT/DOGEUSDT have no");
        System.out.println(" per-symbol override, so their two rows must match exactly, serving as a control on the comparison itself.)");

        for (SymbolFixture symbol : SYMBOLS) {
            RuleThresholds calibratedThresholds = PerSymbolRuleThresholds.forSymbol(symbol.name());
            BacktestReport calibrated = runBareRuleTable(symbol.name() + " [tuning, calibrated]", symbol.tuning(), calibratedThresholds);
            BacktestReport defaultThresholds = runBareRuleTable(symbol.name() + " [tuning, DEFAULT]", symbol.tuning(), RuleThresholds.DEFAULT);
            assertStructurallySane(calibrated);
            assertStructurallySane(defaultThresholds);

            System.out.println();
            System.out.println("  " + symbol.name() + " tuning window:");
            printTally("calibrated (BUY)", DirectionalTally.of(calibrated.buySellDecisionPoints()).buy());
            printTally("DEFAULT (BUY)", DirectionalTally.of(defaultThresholds.buySellDecisionPoints()).buy());
            printTally("calibrated (SELL)", DirectionalTally.of(calibrated.buySellDecisionPoints()).sell());
            printTally("DEFAULT (SELL)", DirectionalTally.of(defaultThresholds.buySellDecisionPoints()).sell());
        }
    }
}
