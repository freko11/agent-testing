package com.autotrade.dashboard.monitoring;

import com.autotrade.dashboard.backtest.Checkpoint;

/**
 * One {@link Checkpoint}'s live-vs-baseline expectancy comparison (E8-F5-S1) — mirrors {@code
 * backtest.CheckpointStats}'s field naming so this report reads the same way as the
 * `docs/CHANGELOG.md` E8-F2-S1/S2 prose it's compared against.
 *
 * @param scored                          how many live audit entries this checkpoint actually
 *                                         resolved (TP/SL crossing or horizon-expired) — excludes
 *                                         entries too recent for this checkpoint's forward horizon
 *                                         to have played out yet.
 * @param liveExpectancyPctAfterCosts      {@code CheckpointStats.expectancyPctAfterCosts()} over
 *                                         this checkpoint's live-scored entries only.
 * @param baselineExpectancyPctAfterCosts  {@link LiveDriftBaseline}'s pinned v2 figure for the
 *                                         same direction/checkpoint.
 * @param driftPct                         {@code live - baseline} — negative means the live rule
 *                                         table is underperforming its original backtest.
 * @param possibleDecay                    {@code true} only when {@code scored} meets the
 *                                         configured minimum sample size AND {@code driftPct} is
 *                                         at or below the configured (negative) decay threshold —
 *                                         a small sample never flags decay on its own, however bad
 *                                         its raw numbers look.
 */
public record CheckpointDrift(Checkpoint checkpoint, int scored, double liveExpectancyPctAfterCosts,
                               double baselineExpectancyPctAfterCosts, double driftPct, boolean possibleDecay) {
}
