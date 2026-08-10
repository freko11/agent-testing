package com.autotrade.dashboard.indicator;

import com.autotrade.dashboard.marketdata.Candle;
import com.autotrade.dashboard.marketdata.MarketClosedException;
import com.autotrade.dashboard.marketdata.MarketDataService;
import com.autotrade.dashboard.marketdata.PriceHistoryResult;
import com.autotrade.dashboard.marketdata.TickerSummary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IndicatorService {

    /** slowPeriod + signalPeriod - 1 for the default MACD(12,26,9) — the binding constraint across all three indicators. */
    public static final int MIN_CANDLES_FOR_INDICATORS = 34;

    private final MarketDataService marketDataService;
    private final IndicatorSnapshotRepository indicatorSnapshotRepository;

    /**
     * Last successful {@link #getChartData} response per (normalized) ticker symbol, in-memory only. Alpaca's
     * candles are daily bars, not intraday, so the last fetch made while the market was open is still legitimate
     * historical data once it closes — not actually "stale" in the data-quality sense, just not fresh. Lets the
     * read-only chart survive {@link MarketClosedException} instead of blanking out; deliberately scoped to this
     * diagnostic view only — {@code /signal} and {@code /indicators} keep the hard market-hours gate untouched.
     */
    private final Map<String, ChartDataResponse> lastGoodChartData = new ConcurrentHashMap<>();

    public IndicatorService(MarketDataService marketDataService, IndicatorSnapshotRepository indicatorSnapshotRepository) {
        this.marketDataService = marketDataService;
        this.indicatorSnapshotRepository = indicatorSnapshotRepository;
    }

    public IndicatorResponse computeIndicators(String symbol, int limit) {
        return computeForSignal(symbol, limit).response();
    }

    /** Same computation as {@link #computeIndicators}, but also returns the persisted {@link IndicatorSnapshot} so
     * callers (e.g. the E2-F3 signal engine) can FK against it without a second snapshot write per request. */
    public IndicatorComputation computeForSignal(String symbol, int limit) {
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
        snapshot.setVolatility(indicators.volatility());
        snapshot.setVolume(indicators.volume());
        snapshot.setVolumeTrend(indicators.volumeTrend());
        indicatorSnapshotRepository.save(snapshot);

        // E8-F3-S3: ADX, computed but not persisted to IndicatorSnapshot/exposed on IndicatorResponse —
        // regime classification is a signal.RegimeGatedRuleEngine concern, not a chart/API-surfaced
        // indicator, matching how the class stayed out of NO_STRONG_SIGNAL's rationale string too.
        BigDecimal adx = AdxCalculator.calculate(candles, AdxCalculator.DEFAULT_PERIOD);

        IndicatorResponse response = IndicatorResponse.from(priceHistory.ticker(), priceHistory.source(), latest, indicators);
        return new IndicatorComputation(response, snapshot, adx);
    }

    /**
     * Candles plus an aligned walk-forward RSI/MA series for the E3-F2-S1 price chart. Never throws
     * {@link InsufficientPriceHistoryException} — below {@link #MIN_CANDLES_FOR_INDICATORS} candles, {@code indicators}
     * is simply empty, since a candles-only chart render is still meaningful. No {@link IndicatorSnapshot} is persisted;
     * this is a read-only diagnostic view, not the audited signal-computation path.
     *
     * <p>On {@link MarketClosedException} (stock ticker, market currently closed), falls back to the last
     * successful response for this symbol with {@code stale=true} rather than propagating — see
     * {@link #lastGoodChartData}. Only when no prior successful fetch exists for the symbol does the exception
     * still propagate, matching the pre-existing 409 {@code MARKET_CLOSED} behavior.
     */
    public ChartDataResponse getChartData(String symbol, int limit) {
        String cacheKey = normalizeSymbol(symbol);
        PriceHistoryResult priceHistory;
        try {
            priceHistory = marketDataService.getPriceHistory(symbol, limit);
        } catch (MarketClosedException e) {
            ChartDataResponse cached = lastGoodChartData.get(cacheKey);
            if (cached != null) {
                return new ChartDataResponse(cached.ticker(), cached.source(), cached.candles(), cached.indicators(), true);
            }
            throw e;
        }
        List<Candle> candles = priceHistory.candles();

        List<ChartIndicatorPoint> points = new ArrayList<>();
        for (int i = MIN_CANDLES_FOR_INDICATORS - 1; i < candles.size(); i++) {
            List<Candle> window = candles.subList(0, i + 1);
            BigDecimal rsi = RsiCalculator.calculate(window, RsiCalculator.DEFAULT_PERIOD);
            MovingAverageResult ma = MovingAverageCrossoverCalculator.calculate(window,
                    MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);
            points.add(new ChartIndicatorPoint(candles.get(i).timestamp(), rsi, ma.shortMa(), ma.longMa()));
        }

        ChartDataResponse response = new ChartDataResponse(TickerSummary.from(priceHistory.ticker()), priceHistory.source(),
                candles, points, false);
        lastGoodChartData.put(cacheKey, response);
        return response;
    }

    private static String normalizeSymbol(String symbol) {
        return symbol == null ? null : symbol.trim().toUpperCase();
    }

    private BigDecimalIndicators compute(List<Candle> candles) {
        var rsi = RsiCalculator.calculate(candles, RsiCalculator.DEFAULT_PERIOD);
        var macd = MacdCalculator.calculate(candles, MacdCalculator.DEFAULT_FAST_PERIOD,
                MacdCalculator.DEFAULT_SLOW_PERIOD, MacdCalculator.DEFAULT_SIGNAL_PERIOD);
        var movingAverage = MovingAverageCrossoverCalculator.calculate(candles,
                MovingAverageCrossoverCalculator.DEFAULT_SHORT_PERIOD, MovingAverageCrossoverCalculator.DEFAULT_LONG_PERIOD);
        var volatility = VolatilityCalculator.calculate(candles, VolatilityCalculator.DEFAULT_PERIOD);
        var volumeTrend = VolumeTrendCalculator.calculate(candles,
                VolumeTrendCalculator.DEFAULT_SHORT_PERIOD, VolumeTrendCalculator.DEFAULT_LONG_PERIOD);
        var volume = candles.get(candles.size() - 1).volume().setScale(4, RoundingMode.HALF_UP);
        return new BigDecimalIndicators(rsi, macd, movingAverage, volatility, volume, volumeTrend);
    }

    record BigDecimalIndicators(java.math.BigDecimal rsi, MacdResult macd, MovingAverageResult movingAverage,
                                 java.math.BigDecimal volatility, java.math.BigDecimal volume,
                                 java.math.BigDecimal volumeTrend) {
    }

    public record IndicatorComputation(IndicatorResponse response, IndicatorSnapshot snapshot, BigDecimal adx) {
    }
}
