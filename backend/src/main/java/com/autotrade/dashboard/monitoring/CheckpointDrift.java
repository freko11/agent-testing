package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.Checkpoint;

/**
 * One {@link Checkpoint}'s live-vs-baseline expectancy comparison (E8-F5-S1) — mirrors {@code
 * backtest.CheckpointStats}'s field naming so this report reads the same way as the
 * `docs/CHANGELOG.md` E8-F2-S1/S2 prose it's compared against.
 *
 * <p>{@code *ExpectancyPctAfterCostsAndFunding}/{@code driftPctAfterFunding} (E8-F5-S2) are the
 * funding-adjusted counterparts to the cost-only fields below, computed from the exact same
 * live-scored {@code CheckpointStats} (via {@code expectancyPctAfterCostsAndFunding()}, which
 * already has the live replay's own {@code avgHoldingDays} available — {@code
 * LiveSignalDriftService.scoreOne} feeds {@code WalkForwardScorer.score}'s {@code
 * DirectionalScoreResult.daysHeld} into the same {@code DirectionalAccumulator} the cost-only
 * figures come from, no separate plumbing needed) rather than a second computation. Presented
 * purely as an additional informational figure: {@code possibleDecay} is still decided from the
 * cost-only {@code driftPct} alone, per the confirmed scope choice documented on {@code
 * LiveSignalDriftService.buildCheckpointDrift} — funding cost is a real, uncalibrated placeholder
 * (see {@code backtest.BacktestConfig#FUNDING_RATE_BPS_PER_PERIOD}'s own Javadoc), so gating an
 * alarm on it directly would mean gating on a number nobody has calibrated, unlike the min-sample-
 * size/decay-threshold pair {@code possibleDecay} already uses which are themselves placeholders
 * but at least govern a cost-only figure with a real (if approximate) basis.
 *
 * @param scored                                     how many live audit entries this checkpoint
 *                                                    actually resolved (TP/SL crossing or
 *                                                    horizon-expired) — excludes entries too
 *                                                    recent for this checkpoint's forward horizon
 *                                                    to have played out yet.
 * @param liveExpectancyPctAfterCosts                 {@code CheckpointStats.expectancyPctAfterCosts()}
 *                                                    over this checkpoint's live-scored entries only.
 * @param baselineExpectancyPctAfterCosts             {@link LiveDriftBaseline}'s pinned current-version
 *                                                    figure for the same direction/checkpoint.
 * @param driftPct                                    {@code live - baseline} — negative means the
 *                                                    live rule table is underperforming its
 *                                                    original backtest.
 * @param possibleDecay                               {@code true} only when {@code scored} meets
 *                                                    the configured minimum sample size AND {@code
 *                                                    driftPct} is at or below the configured
 *                                                    (negative) decay threshold — a small sample
 *                                                    never flags decay on its own, however bad its
 *                                                    raw numbers look.
 * @param liveExpectancyPctAfterCostsAndFunding       {@code
 *                                                    CheckpointStats.expectancyPctAfterCostsAndFunding()}
 *                                                    over this checkpoint's live-scored entries.
 * @param baselineExpectancyPctAfterCostsAndFunding   {@link LiveDriftBaseline}'s pinned
 *                                                    funding-adjusted figure for the same
 *                                                    direction/checkpoint.
 * @param driftPctAfterFunding                        {@code liveExpectancyPctAfterCostsAndFunding
 *                                                    - baselineExpectancyPctAfterCostsAndFunding}
 *                                                    — informational only, does not feed {@code
 *                                                    possibleDecay}.
 */
public record CheckpointDrift(Checkpoint checkpoint, int scored, double liveExpectancyPctAfterCosts,
                               double baselineExpectancyPctAfterCosts, double driftPct, boolean possibleDecay,
                               double liveExpectancyPctAfterCostsAndFunding,
                               double baselineExpectancyPctAfterCostsAndFunding, double driftPctAfterFunding) {
}
