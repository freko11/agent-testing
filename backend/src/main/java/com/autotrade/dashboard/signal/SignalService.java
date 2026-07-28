package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.IndicatorService;
import org.springframework.stereotype.Service;

@Service
public class SignalService {

    private final IndicatorService indicatorService;
    private final SignalCallEntryRepository signalCallEntryRepository;

    public SignalService(IndicatorService indicatorService, SignalCallEntryRepository signalCallEntryRepository) {
        this.indicatorService = indicatorService;
        this.signalCallEntryRepository = signalCallEntryRepository;
    }

    public SignalResponse computeSignal(String symbol, int limit) {
        IndicatorService.IndicatorComputation computation = indicatorService.computeForSignal(symbol, limit);
        IndicatorResponse indicators = computation.response();

        SignalRuleId matchedRule = SignalRuleEngine.evaluate(indicators.rsi(), indicators.macd(),
                indicators.movingAverage(), indicators.volatility(), indicators.volumeTrend());
        HoldTerm holdTerm = HoldTermCalculator.calculate(matchedRule, indicators.volatility());

        SignalCallEntry entry = new SignalCallEntry(computation.snapshot().getTicker(), computation.snapshot(),
                matchedRule, holdTerm);
        signalCallEntryRepository.save(entry);

        return SignalResponse.of(indicators, matchedRule, holdTerm);
    }
}
