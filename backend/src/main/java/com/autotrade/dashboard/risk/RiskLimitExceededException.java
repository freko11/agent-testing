package com.autotrade.dashboard.risk;

/** A trade request breached a configured hard risk cap (leverage or position size, E6-F2-S1) — a policy denial, distinct from {@code InvalidTradeRequestException}'s shape/bounds validation. */
public class RiskLimitExceededException extends RuntimeException {

    public RiskLimitExceededException(String message) {
        super(message);
    }
}
