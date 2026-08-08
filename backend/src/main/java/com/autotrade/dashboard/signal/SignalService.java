package com.autotrade.dashboard.signal;

import com.autotrade.dashboard.indicator.IndicatorResponse;
import com.autotrade.dashboard.indicator.IndicatorService;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
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
        return computeSignalWithProvenance(symbol, limit).response();
    }

    /**
     * Same computation as {@link #computeSignal}, but also returns the exact
     * {@link IndicatorSnapshot} the call was derived from, so callers (e.g.
     * E5's order-submission flow) can FK an {@code Order} against it without
     * a second signal computation/persist per request.
     */
    public SignalComputation computeSignalWithProvenance(String symbol, int limit) {
        IndicatorService.IndicatorComputation computation = indicatorService.computeForSignal(symbol, limit);
        IndicatorResponse indicators = computation.response();

        // E8-F1-S4: resolve thresholds off the persisted, normalized ticker symbol (not the raw
        // `symbol` parameter) so this is robust to caller casing, then delegate to the 6-arg
        // evaluate overload — no SignalRuleEngine signature change needed.
        String normalizedSymbol = computation.snapshot().getTicker().getSymbol();
        SignalRuleEngine.RuleThresholds thresholds = PerSymbolRuleThresholds.forSymbol(normalizedSymbol);

        SignalRuleId matchedRule = SignalRuleEngine.evaluate(indicators.rsi(), indicators.macd(),
                indicators.movingAverage(), indicators.volatility(), indicators.volumeTrend(), thresholds);
        HoldTerm holdTerm = HoldTermCalculator.calculate(matchedRule, indicators.volatility());

        SignalCallEntry entry = new SignalCallEntry(computation.snapshot().getTicker(), computation.snapshot(),
                matchedRule, holdTerm);
        signalCallEntryRepository.save(entry);

        SignalResponse response = SignalResponse.of(indicators, matchedRule, holdTerm);
        return new SignalComputation(response, computation.snapshot());
    }

    public record SignalComputation(SignalResponse response, IndicatorSnapshot snapshot) {
    }
}
