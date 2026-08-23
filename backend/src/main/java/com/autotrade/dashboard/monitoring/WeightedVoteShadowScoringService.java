package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.BacktestConfig;
import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.backtest.DirectionalAccumulator;
import com.autotrade.dashboard.backtest.WalkForwardScorer;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.MacdResult;
import com.autotrade.dashboard.indicator.MovingAverageCrossoverCalculator;
import com.autotrade.dashboard.indicator.MovingAverageRelation;
import com.autotrade.dashboard.indicator.MovingAverageResult;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.signal.PerSymbolRuleThresholds;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
import com.autotrade.dashboard.signal.SignalRuleEngine.RuleThresholds;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine;
import com.autotrade.dashboard.signal.WeightedVoteRuleEngine.IndicatorWeights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * E8-F5-S3: read-only diagnostic that replays every {@code SignalCallEntry} logged in a lookback
 * window through {@link WeightedVoteRuleEngine#evaluate} and reports where its call would have
 * differed from what was actually recorded — the "is E8-F3-S1/S5's weighted-vote engine, tuned
 * and out-of-sample-confirmed only against the two checked-in backtest fixtures, actually any
 * good against real live signal history?" question this story exists to answer, before ever
 * considering wiring it into {@code SignalService}/{@code OrderService}.
 *
 * <p><b>Zero change to the live decision path.</b> This class only reads {@code SignalCallEntry}/
 * {@code IndicatorSnapshot} rows and calls {@code WeightedVoteRuleEngine.evaluate} (unmodified) —
 * it never writes to a signal, order, or audit table, and {@code SignalService}/{@code
 * OrderService} are untouched, per this story's confirmed AC. Mirrors {@code
 * LiveSignalDriftService}'s established shape for exactly this kind of live-replay diagnostic:
 * batch-per-ticker market-data fetch, catch-and-skip on a per-entry reconstruction failure,
 * {@code @Scheduled}/{@code @ConditionalOnProperty}-gated, plus an on-demand controller endpoint.
 *
 * <p><b>Reconstructing {@code WeightedVoteRuleEngine.evaluate}'s inputs from a stored {@code
 * IndicatorSnapshot}.</b> {@code IndicatorSnapshot} persists raw {@code macdLine}/{@code
 * macdSignal}/{@code macdHistogram} and {@code maShort}/{@code maLong}, but not the derived {@code
 * MacdResult#histogramPctOfPrice}/{@code MovingAverageResult#separationPctOfPrice}/{@code
 * MovingAverageResult#relation} fields {@code SignalRuleEngine#computeVotes} actually gates on —
 * {@link #reconstructMacd}/{@link #reconstructMovingAverage} below recompute them from the
 * persisted raw values using the exact same formula, {@code MathContext}, and rounding as {@code
 * MacdCalculator}/{@code MovingAverageCrossoverCalculator} themselves, so replaying a snapshot
 * through the weighted engine reproduces byte-identical votes to what the unweighted engine saw
 * at computation time. {@code rsi}/{@code volatility}/{@code volumeTrend} are used as persisted —
 * no reconstruction needed, they're stored in their final form already.
 *
 * <p>Thresholds are resolved via {@code PerSymbolRuleThresholds.forSymbol} — the same call the
 * live unweighted path (`SignalService.computeSignalWithProvenance`) makes — using each entry's
 * ticker symbol, per this story's AC ("the same inputs the live unweighted path already
 * resolves"). This is deliberately the <em>current</em> threshold table, not whatever thresholds
 * happened to be in effect (a possibly older {@code RULE_TABLE_VERSION}) when an older entry was
 * originally computed; a known, accepted limitation for a Phase-0 diagnostic, surfaced in {@link
 * #KNOWN_LIMITATIONS} below rather than silently assumed away.
 *
 * <p><b>Why exactly four agreement buckets are exhaustive, not an arbitrary choice.</b> Both
 * engines call the exact same {@code SignalRuleEngine#computeVotes} with the exact same
 * reconstructed inputs and thresholds, so {@code bullishCount}/{@code bearishCount} — and
 * therefore the three safety gates and the conflict/dissent gate that run before either engine
 * ever applies weighting — are byte-identical between the two replays of one entry. A call can
 * only ever be bullish-leaning (recorded HOLD or BUY) or bearish-leaning (recorded HOLD or SELL)
 * in both engines at once; a "flip" (recorded BUY, weighted SELL, or vice versa) is structurally
 * impossible given this shared-vote invariant, which is why this story's AC names exactly four
 * buckets ({@link WeightedVoteAgreementBucket#AGREE}, {@link
 * WeightedVoteAgreementBucket#WEIGHTED_ONLY_BUY}, {@link
 * WeightedVoteAgreementBucket#WEIGHTED_ONLY_SELL}, {@link
 * WeightedVoteAgreementBucket#DOWNGRADED_BY_WEIGHTED}) rather than a fifth "disagreement on
 * direction" case. See {@link #classify} for exactly how each bucket resolves.
 *
 * <p><b>Known, documented gap (per this story's AC, not silently omitted): this replay cannot
 * reproduce {@code RegimeGatedRuleEngine#applySellGate}'s (or {@code
 * MaCrossoverSellGate#applySellGate}'s) effect on the recorded call.</b> Both are wired into
 * production for crypto SELL calls, downgrading a raw {@code SignalRuleEngine.evaluate} SELL to
 * {@code NO_STRONG_SIGNAL} (HOLD) when the regime is ranging or the MA-crossover separation is
 * too thin — but {@code IndicatorSnapshot} persists neither ADX/regime data nor a re-runnable
 * record of that gate decision, only the raw indicator values gates like these ultimately read
 * (indirectly, via a fresh candle fetch) rather than store. A recorded HOLD that was actually a
 * gate-downgraded SELL, replayed here, can therefore land in {@link
 * WeightedVoteAgreementBucket#WEIGHTED_ONLY_SELL} for a reason that has nothing to do with
 * weighting at all — the weighted engine (which also never applies these SELL gates) simply
 * agrees with what the <em>raw, ungated</em> rule table would have called. This is a data-model
 * gap independent of this story (flagged in {@link #KNOWN_LIMITATIONS} for whoever picks up a
 * future regime-gate calibration story too), not a bug in this replay — {@code IndicatorSnapshot}
 * would need a new persisted ADX/regime column to close it, out of this story's additive-only,
 * no-Flyway-migration scope.
 *
 * <p><b>Scoring methodology</b>: only disagreements are walk-forward scored (via {@link
 * WalkForwardScorer}, the same TP/SL-aware machinery {@code LiveSignalDriftService} and {@code
 * BacktestHarness} both reuse), per this story's AC — {@link WeightedVoteAgreementBucket#AGREE}
 * is a pure count. {@link WeightedVoteAgreementBucket#DOWNGRADED_BY_WEIGHTED} entries are real
 * recorded BUY/SELL calls and so always carry their own persisted hold-term, scored at their own
 * MIN/MID/MAX checkpoints exactly like {@code LiveSignalDriftService.scoreOne}. {@link
 * WeightedVoteAgreementBucket#WEIGHTED_ONLY_BUY}/{@link WeightedVoteAgreementBucket
 * #WEIGHTED_ONLY_SELL} entries were recorded as HOLD and carry no hold-term at all, so they're
 * scored at the single fixed {@link BacktestConfig#HOLD_REFERENCE_HORIZON_DAYS} reference
 * horizon instead — the same horizon {@code BacktestHarness} already uses for every hold-term-less
 * scoring situation (HOLD calls, per-indicator reads).
 *
 * <p><b>Concludes with a documented recommendation, not a wiring decision</b> — actually switching
 * which engine drives real orders is explicit out-of-scope future work per this story's AC. This
 * class only produces the numbers a human reads to make that call; see {@code docs/CHANGELOG.md}'s
 * E8-F5-S3 entry for the sanity-check findings against this repo's own dev data and the resulting
 * recommendation.
 */
@Component
@ConditionalOnProperty(name = "monitoring.weighted-vote-shadow.enabled", havingValue = "true", matchIfMissing = true)
public class WeightedVoteShadowScoringService {

    private static final Logger log = LoggerFactory.getLogger(WeightedVoteShadowScoringService.class);

    private static final MathContext MC = new MathContext(50);

    /** Candles fetched per ticker — same generous horizon {@code LiveSignalDriftService} uses. */
    private static final int PRICE_HISTORY_LIMIT = 500;

    /** Per this class's own Javadoc: documented, not silently omitted, per this story's AC. */
    static final List<String> KNOWN_LIMITATIONS = List.of(
            "This replay cannot reproduce RegimeGatedRuleEngine.applySellGate's or "
                    + "MaCrossoverSellGate.applySellGate's effect on the recorded call, because "
                    + "IndicatorSnapshot persists neither ADX/regime data nor a re-runnable record of "
                    + "either gate decision. A recorded HOLD that was actually a gate-downgraded SELL "
                    + "can appear in the weightedOnlySell bucket for a reason unrelated to weighting. "
                    + "This is a data-model gap independent of this story, not a bug in this replay.",
            "Thresholds are resolved via PerSymbolRuleThresholds.forSymbol using the CURRENT table, "
                    + "not whatever thresholds were in effect (a possibly older RULE_TABLE_VERSION) when "
                    + "an older entry was originally computed.");

    private final SignalCallEntryRepository signalCallEntryRepository;
    private final MarketDataService marketDataService;
    private final int defaultLookbackDays;

    public WeightedVoteShadowScoringService(SignalCallEntryRepository signalCallEntryRepository,
                                             MarketDataService marketDataService,
                                             @Value("${monitoring.weighted-vote-shadow.lookback-days}") int defaultLookbackDays) {
        this.signalCallEntryRepository = signalCallEntryRepository;
        this.marketDataService = marketDataService;
        this.defaultLookbackDays = defaultLookbackDays;
    }

    @Scheduled(fixedDelayString = "${monitoring.weighted-vote-shadow.fixed-delay-ms}")
    public void scheduledShadowCheck() {
        WeightedVoteShadowReport report = computeShadowReport();
        log.info("weightedVoteShadow lookbackDays={} totalEntriesConsidered={} skippedEntries={} agreeCount={} "
                        + "weightedOnlyBuy(count={}, scored={}, expectancyPctAfterCosts={}) "
                        + "weightedOnlySell(count={}, scored={}, expectancyPctAfterCosts={}) "
                        + "downgradedByWeighted(count={}, scored={})",
                report.lookbackDays(), report.totalEntriesConsidered(), report.skippedEntries(), report.agreeCount(),
                report.weightedOnlyBuy().count(), report.weightedOnlyBuy().scoring().scored(),
                report.weightedOnlyBuy().scoring().expectancyPctAfterCosts(),
                report.weightedOnlySell().count(), report.weightedOnlySell().scoring().scored(),
                report.weightedOnlySell().scoring().expectancyPctAfterCosts(),
                report.downgradedByWeighted().count(), report.downgradedByWeighted().scoring().totalCalls());
    }

    /** {@link #computeShadowReport(int)} using the configured {@code
     * monitoring.weighted-vote-shadow.lookback-days} default. */
    public WeightedVoteShadowReport computeShadowReport() {
        return computeShadowReport(defaultLookbackDays);
    }

    /**
     * Replays every {@code SignalCallEntry} logged in the last {@code lookbackDays} through {@link
     * WeightedVoteRuleEngine#evaluate} and buckets/scores the disagreements. Recomputed fresh on
     * every call — no caching, no persistence, same ephemeral-only treatment as {@code
     * LiveSignalDriftService.computeDrift}.
     */
    public WeightedVoteShadowReport computeShadowReport(int lookbackDays) {
        Instant cutoff = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        List<SignalCallEntry> entries = signalCallEntryRepository.findByCreatedAtAfterOrderByCreatedAtAsc(cutoff);

        int skipped = 0;
        int agree = 0;
        List<SignalCallEntry> weightedOnlyBuyEntries = new ArrayList<>();
        List<SignalCallEntry> weightedOnlySellEntries = new ArrayList<>();
        List<SignalCallEntry> downgradedEntries = new ArrayList<>();

        for (SignalCallEntry entry : entries) {
            try {
                WeightedVoteAgreementBucket bucket = classify(entry);
                switch (bucket) {
                    case AGREE -> agree++;
                    case WEIGHTED_ONLY_BUY -> weightedOnlyBuyEntries.add(entry);
                    case WEIGHTED_ONLY_SELL -> weightedOnlySellEntries.add(entry);
                    case DOWNGRADED_BY_WEIGHTED -> downgradedEntries.add(entry);
                }
            } catch (RuntimeException e) {
                skipped++;
                log.debug("Skipping weighted-vote shadow classification for signal call entry {}: {}",
                        entry.getId(), e.getMessage());
            }
        }

        WeightedVoteBucketOutcome weightedOnlyBuy = scoreSingleHorizonBucket(weightedOnlyBuyEntries, true);
        WeightedVoteBucketOutcome weightedOnlySell = scoreSingleHorizonBucket(weightedOnlySellEntries, false);
        WeightedVoteDowngradeOutcome downgraded = scoreDowngradedBucket(downgradedEntries);

        return new WeightedVoteShadowReport(lookbackDays, entries.size(), skipped, agree, weightedOnlyBuy,
                weightedOnlySell, downgraded, KNOWN_LIMITATIONS);
    }

    /** See this class's own Javadoc for why exactly these four outcomes are exhaustive. */
    private WeightedVoteAgreementBucket classify(SignalCallEntry entry) {
        IndicatorSnapshot snapshot = entry.getIndicatorSnapshot();
        BigDecimal price = snapshot.getPrice();
        MacdResult macd = reconstructMacd(snapshot, price);
        MovingAverageResult movingAverage = reconstructMovingAverage(snapshot, price);
        String symbol = entry.getTicker().getSymbol();
        RuleThresholds thresholds = PerSymbolRuleThresholds.forSymbol(symbol);

        SignalRuleId weightedRuleId = WeightedVoteRuleEngine.evaluate(snapshot.getRsi(), macd, movingAverage,
                snapshot.getVolatility(), snapshot.getVolumeTrend(), thresholds, IndicatorWeights.DEFAULT);
        SignalCall weightedCall = weightedRuleId.call();
        SignalCall recordedCall = entry.getCall();

        if (weightedCall == recordedCall) {
            return WeightedVoteAgreementBucket.AGREE;
        }
        if (weightedCall == SignalCall.BUY) {
            return WeightedVoteAgreementBucket.WEIGHTED_ONLY_BUY;
        }
        if (weightedCall == SignalCall.SELL) {
            return WeightedVoteAgreementBucket.WEIGHTED_ONLY_SELL;
        }
        // weightedCall == HOLD here, and the equality check above already ruled out recordedCall
        // == HOLD too, so recordedCall must be BUY or SELL.
        return WeightedVoteAgreementBucket.DOWNGRADED_BY_WEIGHTED;
    }

    /** Same formula/{@code MathContext}/rounding as {@code MacdCalculator.calculate}, applied to
     * {@code snapshot}'s already-persisted raw {@code macdHistogram} rather than recomputed from
     * candles — see this class's own Javadoc. */
    private static MacdResult reconstructMacd(IndicatorSnapshot snapshot, BigDecimal price) {
        BigDecimal histogram = snapshot.getMacdHistogram();
        BigDecimal histogramPctOfPrice = histogram.abs().divide(price, MC)
                .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
        return new MacdResult(snapshot.getMacdLine(), snapshot.getMacdSignal(), histogram, histogramPctOfPrice);
    }

    /** Same formula/{@code MathContext}/rounding as {@code MovingAverageCrossoverCalculator
     * .calculate}, applied to {@code snapshot}'s already-persisted raw {@code maShort}/{@code
     * maLong} rather than recomputed from candles — see this class's own Javadoc. */
    private static MovingAverageResult reconstructMovingAverage(IndicatorSnapshot snapshot, BigDecimal price) {
        BigDecimal shortMa = snapshot.getMaShort();
        BigDecimal longMa = snapshot.getMaLong();
        int cmp = shortMa.compareTo(longMa);
        MovingAverageRelation relation = cmp > 0 ? MovingAverageRelation.SHORT_ABOVE_LONG
                : cmp < 0 ? MovingAverageRelation.SHORT_BELOW_LONG : MovingAverageRelation.EQUAL;
        BigDecimal separationPctOfPrice = shortMa.subtract(longMa).abs().divide(price, MC)
                .multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
        return new MovingAverageResult(MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, shortMa,
                MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD, longMa, relation, separationPctOfPrice);
    }

    private WeightedVoteBucketOutcome scoreSingleHorizonBucket(List<SignalCallEntry> bucketed, boolean isBuy) {
        WeightedVoteSingleHorizonAccumulator acc = new WeightedVoteSingleHorizonAccumulator();
        for (Map.Entry<String, List<SignalCallEntry>> tickerGroup : groupByTickerSymbol(bucketed).entrySet()) {
            List<Candle> candles = fetchCandlesOrNull(tickerGroup.getKey());
            if (candles == null) {
                continue;
            }
            for (SignalCallEntry entry : tickerGroup.getValue()) {
                try {
                    scoreOneFixedHorizon(entry, candles, isBuy, acc);
                } catch (RuntimeException e) {
                    log.debug("Skipping weighted-vote shadow scoring for signal call entry {}: {}",
                            entry.getId(), e.getMessage());
                }
            }
        }
        return new WeightedVoteBucketOutcome(bucketed.size(), acc.toStats());
    }

    private void scoreOneFixedHorizon(SignalCallEntry entry, List<Candle> candles, boolean isBuy,
                                       WeightedVoteSingleHorizonAccumulator acc) {
        IndicatorSnapshot snapshot = entry.getIndicatorSnapshot();
        BigDecimal decisionClose = snapshot.getPrice();
        Instant decisionAt = snapshot.getSnapshotAt();
        List<Candle> forward = candles.stream().filter(candle -> candle.timestamp().isAfter(decisionAt)).toList();

        int horizon = BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS;
        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward, horizon, decisionClose, isBuy);
        acc.record(WalkForwardScorer.score(forward, horizon, decisionClose, isBuy, crossing));
    }

    private WeightedVoteDowngradeOutcome scoreDowngradedBucket(List<SignalCallEntry> bucketed) {
        DirectionalAccumulator acc = new DirectionalAccumulator();
        for (Map.Entry<String, List<SignalCallEntry>> tickerGroup : groupByTickerSymbol(bucketed).entrySet()) {
            List<Candle> candles = fetchCandlesOrNull(tickerGroup.getKey());
            if (candles == null) {
                continue;
            }
            for (SignalCallEntry entry : tickerGroup.getValue()) {
                try {
                    scoreOneDowngraded(entry, candles, acc);
                } catch (RuntimeException e) {
                    log.debug("Skipping weighted-vote shadow scoring for signal call entry {}: {}",
                            entry.getId(), e.getMessage());
                }
            }
        }
        return new WeightedVoteDowngradeOutcome(bucketed.size(), acc.toStats());
    }

    private void scoreOneDowngraded(SignalCallEntry entry, List<Candle> candles, DirectionalAccumulator acc) {
        boolean isBuy = entry.getCall() == SignalCall.BUY;
        Integer minDays = entry.getHoldTermMinDays();
        Integer maxDays = entry.getHoldTermMaxDays();
        if (minDays == null || maxDays == null) {
            log.debug("Skipping downgraded-bucket scoring for signal call entry {}: no hold-term recorded",
                    entry.getId());
            return;
        }
        int midDays = (int) Math.round((minDays + maxDays) / 2.0);

        IndicatorSnapshot snapshot = entry.getIndicatorSnapshot();
        BigDecimal decisionClose = snapshot.getPrice();
        Instant decisionAt = snapshot.getSnapshotAt();
        List<Candle> forward = candles.stream().filter(candle -> candle.timestamp().isAfter(decisionAt)).toList();

        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward, maxDays, decisionClose, isBuy);
        acc.totalCalls++;
        acc.record(Checkpoint.MIN, WalkForwardScorer.score(forward, minDays, decisionClose, isBuy, crossing));
        acc.record(Checkpoint.MID, WalkForwardScorer.score(forward, midDays, decisionClose, isBuy, crossing));
        acc.record(Checkpoint.MAX, WalkForwardScorer.score(forward, maxDays, decisionClose, isBuy, crossing));
    }

    private Map<String, List<SignalCallEntry>> groupByTickerSymbol(List<SignalCallEntry> entries) {
        return entries.stream().collect(Collectors.groupingBy(entry -> entry.getTicker().getSymbol()));
    }

    /** {@code null} (not an exception) when this ticker's forward market data can't be fetched —
     * the same "one ticker's outage never aborts the rest of the run" contract {@code
     * LiveSignalDriftService} already follows. */
    private List<Candle> fetchCandlesOrNull(String symbol) {
        try {
            return marketDataService.getPriceHistory(symbol, PRICE_HISTORY_LIMIT).candles();
        } catch (RuntimeException e) {
            log.debug("Skipping weighted-vote shadow scoring for {}: {}", symbol, e.getMessage());
            return null;
        }
    }
}
