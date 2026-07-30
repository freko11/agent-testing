package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;
import jakarta.validation.constraints.NotNull;

public record TradingModeChangeRequest(@NotNull TradingMode mode) {
}
