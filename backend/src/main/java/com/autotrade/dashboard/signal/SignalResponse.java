package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.marketdata.TickerSummary;

public record SignalResponse(TickerSummary ticker, SignalCall call, String matchedRule, String ruleRationale,
                              String ruleTableVersion, HoldTerm holdTerm, IndicatorResponse indicators) {

    static SignalResponse of(IndicatorResponse indicators, SignalRuleId matchedRule, HoldTerm holdTerm) {
        return new SignalResponse(indicators.ticker(), matchedRule.call(), matchedRule.name(),
                matchedRule.rationale(), SignalRuleEngine.RULE_TABLE_VERSION, holdTerm, indicators);
    }
}
