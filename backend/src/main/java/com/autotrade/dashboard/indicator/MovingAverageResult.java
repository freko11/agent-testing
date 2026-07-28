package com.autotrade.dashboard.indicator;

import java.math.BigDecimal;

public record MovingAverageResult(int shortPeriod, BigDecimal shortMa, int longPeriod, BigDecimal longMa,
                                   MovingAverageRelation relation) {
}
