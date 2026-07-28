package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.marketdata.PriceHistoryResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndicatorService {

    /** slowPeriod + signalPeriod - 1 for the default MACD(12,26,9) — the binding constraint across all three indicators. */
    public static final int MIN_CANDLES_FOR_INDICATORS = 34;

    private final MarketDataService marketDataService;
    private final IndicatorSnapshotRepository indicatorSnapshotRepository;

    public IndicatorService(MarketDataService marketDataService, IndicatorSnapshotRepository indicatorSnapshotRepository) {
        this.marketDataService = marketDataService;
        this.indicatorSnapshotRepository = indicatorSnapshotRepository;
    }

    public IndicatorResponse computeIndicators(String symbol, int limit) {
        PriceHistoryResult priceHistory = marketDataService.getPriceHistory(symbol, limit);
        List<Candle> candles = priceHistory.candles();

        if (candles.size() < MIN_CANDLES_FOR_INDICATORS) {
            throw new InsufficientPriceHistoryException(
                    "\"" + symbol + "\" has only " + candles.size() + " candle(s); at least "
                            + MIN_CANDLES_FOR_INDICATORS + " are required to compute indicators.");
        }

        BigDecimalIndicators indicators = compute(candles);
        Candle latest = candles.get(candles.size() - 1);

        IndicatorSnapshot snapshot = new IndicatorSnapshot(priceHistory.ticker(), latest.timestamp(), latest.close(),
                priceHistory.source());
        snapshot.setRsi(indicators.rsi());
        snapshot.setMacdLine(indicators.macd().line());
        snapshot.setMacdSignal(indicators.macd().signal());
        snapshot.setMacdHistogram(indicators.macd().histogram());
        snapshot.setMaShort(indicators.movingAverage().shortMa());
        snapshot.setMaLong(indicators.movingAverage().longMa());
        indicatorSnapshotRepository.save(snapshot);

        return IndicatorResponse.from(priceHistory.ticker(), priceHistory.source(), latest, indicators);
    }

    private BigDecimalIndicators compute(List<Candle> candles) {
        var rsi = RsiCalculator.calculate(candles, RsiCalculator.DEFAULT_PERIOD);
        var macd = MacdCalculator.calculate(candles, MacdCalculator.DEFAULT_FAST_PERIOD,
                MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD);
        var movingAverage = MovingAverageCrossoverCalculator.calculate(candles,
                MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);
        return new BigDecimalIndicators(rsi, macd, movingAverage);
    }

    record BigDecimalIndicators(java.math.BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage) {
    }
}
