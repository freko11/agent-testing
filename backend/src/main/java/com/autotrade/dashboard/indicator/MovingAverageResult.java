package com.autotrade.dashboard.indicator;

import java.math.BigDecimal;

/**
 * {@code separationPctOfPrice} (E8-F1-S6) is {@code |shortMa - longMa| / lastClose * 100} — the
 * short/long MA gap's raw price-unit magnitude is meaningless as a single global rule-engine
 * threshold across tickers of very different price scales (BTCUSDT vs. DOGEUSDT), so it's
 * normalized against the candle series' last close the same way {@link MacdResult#histogramPctOfPrice}
 * (E8-F1-S5) normalizes the MACD histogram, for direct cross-symbol comparability with that
 * precedent's own normalization basis.
 */
public record MovingAverageResult(int shortPeriod, BigDecimal shortMa, int longPeriod, BigDecimal longMa,
                                   MovingAverageRelation relation, BigDecimal separationPctOfPrice) {
}
