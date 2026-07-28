package com.autotrade.dashboard.indicator;

import java.math.BigDecimal;

public record MacdResult(BigDecimal line, BigDecimal signal, BigDecimal histogram) {
}
