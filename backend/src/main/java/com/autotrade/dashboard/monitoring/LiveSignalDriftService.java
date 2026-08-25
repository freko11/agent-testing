package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.alert.SystemAlertService;
import com.autotrade.dashboard.backtest.Checkpoint;
import com.autotrade.dashboard.backtest.CheckpointStats;
import com.autotrade.dashboard.backtest.DirectionalAccumulator;
import com.autotrade.dashboard.backtest.DirectionalOutcomeStats;
import com.autotrade.dashboard.backtest.DirectionalScoreResult;
import com.autotrade.dashboard.backtest.WalkForwardScorer;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.order.OrderAuditEntry;
import com.autotrade.dashboard.order.OrderAuditEntryRepository;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Re-scores the rule table's <b>live</b> performance against production {@code OrderAuditEntry}
 * rows, grouped by {@code rule_table_version}, and compares it to {@link LiveDriftBaseline}'s
 * pinned original-backtest figures — E8-F5-S1's "detect the rule table's edge decaying in live
 * markets before it costs real money" AC.
 *
 * <p><b>Design gate finding this class exists to work around</b>: neither {@code Order} nor
 * {@code OrderAuditEntry} record a trade's exit (no exit price, no TP/SL-hit flag, no close
 * timestamp) — E4-F3-S2/E6-F2-S2 deliberately stopped at "entry filled, protection legs resting
 * on the broker." So scoring a live decision means re-fetching real forward market data after its
 * decision point and running the exact same TP/SL walk-forward scan {@code backtest.BacktestHarness}
 * already runs against a fixture ({@link WalkForwardScorer}, promoted to main scope by this same
 * story specifically so this class can reuse it) — not reading a recorded outcome that doesn't
 * exist.
 *
 * <p><b>Confirmed scope</b> (design-gated before implementation): backend-only, ephemeral
 * computation only (no new table, no persisted report — every call recomputes fresh from {@code
 * OrderAuditEntry} and real market data), rescoring only {@code resultStatus} in {@code {FILLED,
 * PARTIALLY_PROTECTED}} (both mean the entry leg actually filled — real market exposure existed;
 * {@code REJECTED}/{@code FAILED}/{@code SUBMISSION_UNKNOWN} never did), and a baseline computed
 * for only the CURRENT {@code SignalRuleEngine.RULE_TABLE_VERSION} (see {@link
 * LiveDriftBaseline}) — an older/newer version's live calls still surface raw counts, just
 * without a fabricated baseline comparison.
 *
 * <p>Batches {@code MarketDataService.getPriceHistory} once per distinct ticker symbol (not once
 * per audit entry) — the same rate-limit hygiene {@code notification.WatchlistSignalPoller}
 * already established — and catches/logs/skips per-ticker market-data failures (market closed,
 * ticker deregistered, broker/market-data errors) without aborting the rest of the run, the same
 * "one failure never stops the batch" contract {@code WatchlistSignalPoller.pollOne}'s catch
 * block already follows.
 *
 * <p>Gated on a minimum sample size and a decay threshold (both config, both explicitly
 * uncalibrated placeholders — see {@code application.properties}) before flagging {@code
 * possibleDecay} on any checkpoint, so a two-trade sample never triggers a false alarm; raw call
 * counts are always surfaced alongside percentages so a near-empty audit log reads as "no data
 * yet," not "flat performance."
 *
 * <p>E8-F5-S2 added a funding-adjusted comparison alongside the cost-only one above, reusing the
 * same live-scored {@code CheckpointStats} (its {@code avgHoldingDays} already comes from {@code
 * WalkForwardScorer.score}'s own {@code DirectionalScoreResult.daysHeld}, no separate plumbing
 * needed) against {@link LiveDriftBaseline}'s new funding-adjusted constants — see {@link
 * #buildCheckpointDrift} for why {@code possibleDecay} still gates on the cost-only figure alone.
 */
@Component
@ConditionalOnProperty(name = "monitoring.live-drift.enabled", havingValue = "true", matchIfMissing = true)
public class LiveSignalDriftService {

    private static final Logger log = LoggerFactory.getLogger(LiveSignalDriftService.class);

    /** Both mean the entry leg actually filled — real market exposure existed. {@code REJECTED}/
     * {@code FAILED}/{@code SUBMISSION_UNKNOWN} audit rows never reached that state and are never
     * rescored. */
    private static final List<OrderStatus> EXPOSURE_STATUSES = List.of(OrderStatus.FILLED, OrderStatus.PARTIALLY_PROTECTED);

    /** Candles fetched per ticker — generous enough to comfortably cover any hold-term's max-day
     * horizon plus however far in the past the lookback window reaches. */
    private static final int PRICE_HISTORY_LIMIT = 500;

    private final OrderAuditEntryRepository orderAuditEntryRepository;
    private final MarketDataService marketDataService;
    private final SystemAlertService systemAlertService;
    private final int defaultLookbackDays;
    private final int minSampleSize;
    private final double decayThresholdPct;

    public LiveSignalDriftService(OrderAuditEntryRepository orderAuditEntryRepository,
                                   MarketDataService marketDataService,
                                   SystemAlertService systemAlertService,
                                   @Value("${monitoring.live-drift.lookback-days}") int defaultLookbackDays,
                                   @Value("${monitoring.live-drift.min-sample-size}") int minSampleSize,
                                   @Value("${monitoring.live-drift.decay-threshold-pct}") double decayThresholdPct) {
        this.orderAuditEntryRepository = orderAuditEntryRepository;
        this.marketDataService = marketDataService;
        this.systemAlertService = systemAlertService;
        this.defaultLookbackDays = defaultLookbackDays;
        this.minSampleSize = minSampleSize;
        this.decayThresholdPct = decayThresholdPct;
    }

    @Scheduled(fixedDelayString = "${monitoring.live-drift.fixed-delay-ms}")
    public void scheduledDriftCheck() {
        SignalDriftReport report = computeDrift();
        for (RuleTableVersionDrift version : report.versions()) {
            logDirection(version.ruleTableVersion(), "BUY", version.buy());
            logDirection(version.ruleTableVersion(), "SELL", version.sell());
        }
    }

    private void logDirection(String ruleTableVersion, String direction, DirectionalDrift drift) {
        for (CheckpointDrift checkpoint : drift.checkpoints()) {
            if (checkpoint.possibleDecay()) {
                log.warn("ruleTableVersion={} direction={} checkpoint={} scored={} liveExpectancyPctAfterCosts={} "
                                + "baselineExpectancyPctAfterCosts={} driftPct={} "
                                + "liveExpectancyPctAfterCostsAndFunding={} baselineExpectancyPctAfterCostsAndFunding={} "
                                + "driftPctAfterFunding={} - possible signal decay detected",
                        ruleTableVersion, direction, checkpoint.checkpoint(), checkpoint.scored(),
                        checkpoint.liveExpectancyPctAfterCosts(), checkpoint.baselineExpectancyPctAfterCosts(),
                        checkpoint.driftPct(), checkpoint.liveExpectancyPctAfterCostsAndFunding(),
                        checkpoint.baselineExpectancyPctAfterCostsAndFunding(), checkpoint.driftPctAfterFunding());
                systemAlertService.recordSignalDriftDecay(ruleTableVersion, direction, checkpoint.checkpoint(),
                        checkpoint.driftPct());
            } else {
                log.info("ruleTableVersion={} direction={} checkpoint={} scored={} liveExpectancyPctAfterCosts={} "
                                + "baselineExpectancyPctAfterCosts={} driftPct={} "
                                + "liveExpectancyPctAfterCostsAndFunding={} baselineExpectancyPctAfterCostsAndFunding={} "
                                + "driftPctAfterFunding={}",
                        ruleTableVersion, direction, checkpoint.checkpoint(), checkpoint.scored(),
                        checkpoint.liveExpectancyPctAfterCosts(), checkpoint.baselineExpectancyPctAfterCosts(),
                        checkpoint.driftPct(), checkpoint.liveExpectancyPctAfterCostsAndFunding(),
                        checkpoint.baselineExpectancyPctAfterCostsAndFunding(), checkpoint.driftPctAfterFunding());
            }
        }
    }

    /** {@link #computeDrift(int)} using the configured {@code monitoring.live-drift.lookback-days}
     * default — what both the {@code @Scheduled} job and a parameterless controller call use. */
    public SignalDriftReport computeDrift() {
        return computeDrift(defaultLookbackDays);
    }

    /**
     * Replays every {@code FILLED}/{@code PARTIALLY_PROTECTED} {@code OrderAuditEntry} logged in
     * the last {@code lookbackDays} against real forward market data, grouped by {@code
     * rule_table_version} and direction. Recomputed fresh on every call — no caching, no
     * persistence, per this story's confirmed ephemeral-only scope.
     */
    public SignalDriftReport computeDrift(int lookbackDays) {
        Instant cutoff = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        List<OrderAuditEntry> entries =
                orderAuditEntryRepository.findByResultStatusInAndLoggedAtAfterOrderByLoggedAtAsc(EXPOSURE_STATUSES, cutoff);

        Map<String, List<OrderAuditEntry>> byTickerSymbol = entries.stream()
                .collect(Collectors.groupingBy(entry -> entry.getTicker().getSymbol()));

        // ruleTableVersion -> isBuy -> accumulator
        Map<String, Map<Boolean, DirectionalAccumulator>> accumulators = new HashMap<>();
        int scored = 0;
        int skipped = 0;

        for (Map.Entry<String, List<OrderAuditEntry>> tickerGroup : byTickerSymbol.entrySet()) {
            String symbol = tickerGroup.getKey();
            List<OrderAuditEntry> tickerEntries = tickerGroup.getValue();
            List<Candle> candles;
            try {
                candles = marketDataService.getPriceHistory(symbol, PRICE_HISTORY_LIMIT).candles();
            } catch (RuntimeException e) {
                skipped += tickerEntries.size();
                log.debug("Skipping live signal drift scoring for {} ({} audit entries): {}",
                        symbol, tickerEntries.size(), e.getMessage());
                continue;
            }

            for (OrderAuditEntry auditEntry : tickerEntries) {
                try {
                    if (scoreOne(auditEntry, candles, accumulators)) {
                        scored++;
                    } else {
                        skipped++;
                    }
                } catch (RuntimeException e) {
                    skipped++;
                    log.debug("Skipping live signal drift scoring for audit entry {}: {}", auditEntry.getId(), e.getMessage());
                }
            }
        }

        List<RuleTableVersionDrift> versions = accumulators.entrySet().stream()
                .map(entry -> buildVersionDrift(entry.getKey(), entry.getValue()))
                .toList();

        return new SignalDriftReport(lookbackDays, entries.size(), scored, skipped, versions);
    }

    /**
     * Scores one audit entry's decision against real forward market data, using the frozen
     * BUY/SELL call and hold-term already recorded on its {@code SignalCallEntry} (E6-F3-S1/S2) —
     * never the order's broker fill price, so this measures rule-table directional-edge decay,
     * apples-to-apples with how {@code BacktestHarness} itself scores, not execution quality.
     *
     * @return {@code true} if the entry was accumulated (even if none of its individual
     *         checkpoints could be scored yet — not enough forward time has passed), {@code
     *         false} if it was skipped outright (no hold-term recorded on its {@code
     *         SignalCallEntry}).
     */
    private boolean scoreOne(OrderAuditEntry auditEntry, List<Candle> candles,
                              Map<String, Map<Boolean, DirectionalAccumulator>> accumulators) {
        SignalCallEntry signalCallEntry = auditEntry.getSignalCallEntry();
        // OrderService.submitOrder throws SignalNotActionableException for a HOLD call before any
        // Order/OrderAuditEntry row is ever created, so this is always BUY or SELL here.
        boolean isBuy = signalCallEntry.getCall() == SignalCall.BUY;

        Integer minDays = signalCallEntry.getHoldTermMinDays();
        Integer maxDays = signalCallEntry.getHoldTermMaxDays();
        if (minDays == null || maxDays == null) {
            log.debug("Skipping audit entry {}: its SignalCallEntry has no hold-term recorded", auditEntry.getId());
            return false;
        }
        int midDays = (int) Math.round((minDays + maxDays) / 2.0);

        IndicatorSnapshot snapshot = signalCallEntry.getIndicatorSnapshot();
        BigDecimal decisionClose = snapshot.getPrice();
        Instant decisionAt = snapshot.getSnapshotAt();
        List<Candle> forward = candles.stream().filter(candle -> candle.timestamp().isAfter(decisionAt)).toList();

        Optional<WalkForwardScorer.CrossingEvent> crossing =
                WalkForwardScorer.findFirstCrossing(forward, maxDays, decisionClose, isBuy);
        Optional<DirectionalScoreResult> minResult = WalkForwardScorer.score(forward, minDays, decisionClose, isBuy, crossing);
        Optional<DirectionalScoreResult> midResult = WalkForwardScorer.score(forward, midDays, decisionClose, isBuy, crossing);
        Optional<DirectionalScoreResult> maxResult = WalkForwardScorer.score(forward, maxDays, decisionClose, isBuy, crossing);

        DirectionalAccumulator acc = accumulators
                .computeIfAbsent(signalCallEntry.getRuleTableVersion(), k -> new HashMap<>())
                .computeIfAbsent(isBuy, k -> new DirectionalAccumulator());
        acc.totalCalls++;
        acc.record(Checkpoint.MIN, minResult);
        acc.record(Checkpoint.MID, midResult);
        acc.record(Checkpoint.MAX, maxResult);
        return true;
    }

    private RuleTableVersionDrift buildVersionDrift(String ruleTableVersion, Map<Boolean, DirectionalAccumulator> byDirection) {
        boolean hasBaseline = LiveDriftBaseline.RULE_TABLE_VERSION.equals(ruleTableVersion);
        DirectionalDrift buy = buildDirectionalDrift(true, byDirection.get(Boolean.TRUE), hasBaseline);
        DirectionalDrift sell = buildDirectionalDrift(false, byDirection.get(Boolean.FALSE), hasBaseline);
        return new RuleTableVersionDrift(ruleTableVersion, hasBaseline, buy, sell);
    }

    private DirectionalDrift buildDirectionalDrift(boolean isBuy, DirectionalAccumulator acc, boolean hasBaseline) {
        if (acc == null) {
            return new DirectionalDrift(0, List.of());
        }
        DirectionalOutcomeStats stats = acc.toStats();
        if (!hasBaseline) {
            return new DirectionalDrift(stats.totalCalls(), List.of());
        }
        List<CheckpointDrift> checkpoints = new ArrayList<>();
        for (Checkpoint checkpoint : Checkpoint.values()) {
            checkpoints.add(buildCheckpointDrift(isBuy, checkpoint, checkpointStats(stats, checkpoint)));
        }
        return new DirectionalDrift(stats.totalCalls(), checkpoints);
    }

    /**
     * {@code possibleDecay} (E8-F5-S1) is decided from the cost-only {@code driftPct} alone, not
     * the funding-adjusted one added by E8-F5-S2 — extended, not replaced, per that story's
     * confirmed scope: funding-adjusted drift is surfaced on {@link CheckpointDrift} purely as an
     * additional informational figure (see that record's own Javadoc for why), reusing the exact
     * same {@code minSampleSize}/{@code decayThresholdPct} gate would mean applying an already-
     * uncalibrated threshold to a doubly-uncalibrated figure (funding rate on top of transaction
     * cost) with no basis for picking a different threshold for it — the cost-only comparison
     * stays the one signal that actually trips an alarm.
     */
    private CheckpointDrift buildCheckpointDrift(boolean isBuy, Checkpoint checkpoint, CheckpointStats cp) {
        double live = cp.expectancyPctAfterCosts();
        double baseline = LiveDriftBaseline.expectancyPctAfterCosts(isBuy, checkpoint);
        double driftPct = live - baseline;
        boolean possibleDecay = cp.scored() >= minSampleSize && driftPct <= -decayThresholdPct;

        double liveWithFunding = cp.expectancyPctAfterCostsAndFunding();
        double baselineWithFunding = LiveDriftBaseline.expectancyPctAfterCostsAndFunding(isBuy, checkpoint);
        double driftPctAfterFunding = liveWithFunding - baselineWithFunding;

        return new CheckpointDrift(checkpoint, cp.scored(), live, baseline, driftPct, possibleDecay,
                liveWithFunding, baselineWithFunding, driftPctAfterFunding);
    }

    private CheckpointStats checkpointStats(DirectionalOutcomeStats stats, Checkpoint checkpoint) {
        return switch (checkpoint) {
            case MIN -> stats.min();
            case MID -> stats.mid();
            case MAX -> stats.max();
        };
    }
}
