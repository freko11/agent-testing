package com.autotrade.dashboard.backtest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E8-F2-S2: pins {@link CheckpointStats#expectancyPctAfterCosts()}'s arithmetic down exactly
 * against hand-constructed records — no need to go through {@link BacktestHarness}'s
 * accumulator/combine machinery, since the method is a pure function of the record's own fields
 * plus the fixed {@link BacktestConfig#TRANSACTION_COST_BPS} constant.
 *
 * <p>E8-F2-S3: also pins {@link CheckpointStats#expectancyPctAfterCostsAndFunding()}'s arithmetic
 * — unlike the flat {@code TRANSACTION_COST_BPS}, funding cost scales with {@code avgHoldingDays},
 * so the pre-existing tests above pass {@code 0.0} for that field (funding cost is zero when a
 * position is never held) and stay unaffected by this story's addition.
 */
class CheckpointStatsTest {

    private static final double DELTA = 1e-9;

    @Test
    void expectancyPctAfterCosts_subtractsFlatRoundTripCostFromRawExpectancy() {
        CheckpointStats cp = new CheckpointStats(3, 1, 0, 0, 2.0, -4.0, 0, 0, 0, 0.0);

        assertEquals(0.5, cp.expectancyPct(), DELTA);
        assertEquals(0.30, cp.expectancyPctAfterCosts(), DELTA);
    }

    @Test
    void expectancyPctAfterCosts_isZeroWhenNothingScored() {
        CheckpointStats cp = new CheckpointStats(0, 0, 0, 0, 0.0, 0.0, 0, 0, 0, 0.0);

        assertEquals(0.0, cp.expectancyPctAfterCosts(), DELTA);
    }

    @Test
    void expectancyPctAfterCosts_canFlipPositiveExpectancyNegative() {
        CheckpointStats cp = new CheckpointStats(6, 4, 0, 0, 0.5, -0.4, 0, 0, 0, 0.0);

        assertEquals(0.14, cp.expectancyPct(), DELTA);
        assertTrue(cp.expectancyPct() > 0, "raw expectancy should be positive on paper");
        assertEquals(-0.06, cp.expectancyPctAfterCosts(), DELTA);
        assertTrue(cp.expectancyPctAfterCosts() < 0,
                "after-cost expectancy should flip negative once transaction costs are paid");
    }

    @Test
    void expectancyPctAfterCosts_neverExceedsRawExpectancy() {
        CheckpointStats winHeavy = new CheckpointStats(8, 2, 0, 0, 1.0, -1.0, 0, 0, 0, 0.0);
        CheckpointStats lossHeavy = new CheckpointStats(2, 8, 0, 0, 1.0, -1.0, 0, 0, 0, 0.0);

        assertTrue(winHeavy.expectancyPctAfterCosts() <= winHeavy.expectancyPct());
        assertTrue(lossHeavy.expectancyPctAfterCosts() <= lossHeavy.expectancyPct());
    }

    @Test
    void expectancyPctAfterCostsAndFunding_subtractsDurationScaledFundingCost() {
        // avgHoldingDays=5.0: fundingCostPct = (3bps/100) * (24/8 periods/day) * 5.0 days = 0.45%
        CheckpointStats cp = new CheckpointStats(3, 1, 0, 0, 2.0, -4.0, 0, 0, 0, 5.0);

        assertEquals(0.30, cp.expectancyPctAfterCosts(), DELTA);
        assertEquals(-0.15, cp.expectancyPctAfterCostsAndFunding(), DELTA);
    }

    @Test
    void expectancyPctAfterCostsAndFunding_isZeroWhenNothingScored() {
        CheckpointStats cp = new CheckpointStats(0, 0, 0, 0, 0.0, 0.0, 0, 0, 0, 0.0);

        assertEquals(0.0, cp.expectancyPctAfterCostsAndFunding(), DELTA);
    }

    @Test
    void expectancyPctAfterCostsAndFunding_neverExceedsAfterCostsExpectancy() {
        CheckpointStats cp = new CheckpointStats(8, 2, 0, 0, 1.0, -1.0, 0, 0, 0, 3.0);

        assertTrue(cp.expectancyPctAfterCostsAndFunding() <= cp.expectancyPctAfterCosts());
    }

    @Test
    void expectancyPctAfterCostsAndFunding_scalesWithHoldingDurationNotFlat() {
        CheckpointStats shortHold = new CheckpointStats(6, 4, 0, 0, 0.5, -0.4, 0, 0, 0, 1.0);
        CheckpointStats longHold = new CheckpointStats(6, 4, 0, 0, 0.5, -0.4, 0, 0, 0, 10.0);

        assertEquals(shortHold.expectancyPctAfterCosts(), longHold.expectancyPctAfterCosts(), DELTA,
                "same win/loss shape should give identical after-costs expectancy regardless of holding duration");
        assertTrue(longHold.expectancyPctAfterCostsAndFunding() < shortHold.expectancyPctAfterCostsAndFunding(),
                "funding cost should scale with avg holding duration, not be flat per trade");
    }
}
