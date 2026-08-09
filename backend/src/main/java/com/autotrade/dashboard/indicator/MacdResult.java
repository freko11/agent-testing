package com.autotrade.dashboard.indicator;

import java.math.BigDecimal;

/**
 * {@code histogramPctOfPrice} (E8-F1-S5) is {@code |histogram| / lastClose * 100} — the
 * histogram's raw price-unit magnitude is meaningless as a single global rule-engine threshold
 * across tickers of very different price scales (BTCUSDT vs. DOGEUSDT), so it's normalized here
 * the same way {@link VolatilityCalculator} already normalizes ATR to a percentage of price,
 * rather than threading a separate price parameter through every {@code SignalRuleEngine}/
 * {@code WeightedVoteRuleEngine} call site.
 */
public record MacdResult(BigDecimal line, BigDecimal signal, BigDecimal histogram, BigDecimal histogramPctOfPrice) {
}
