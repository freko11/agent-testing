package com.autotrade.dashboard.indicator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickers")
public class IndicatorController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final IndicatorService indicatorService;

    public IndicatorController(IndicatorService indicatorService) {
        this.indicatorService = indicatorService;
    }

    @GetMapping("/{symbol}/indicators")
    public IndicatorResponse indicators(@PathVariable String symbol,
                                         @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        if (limit < IndicatorService.MIN_CANDLES_FOR_INDICATORS || limit > MAX_LIMIT) {
            throw new InvalidIndicatorRequestException("limit must be between " + IndicatorService.MIN_CANDLES_FOR_INDICATORS
                    + " and " + MAX_LIMIT + ", got " + limit);
        }
        return indicatorService.computeIndicators(symbol, limit);
    }

    /**
     * Candles plus an aligned RSI/MA series for the E3-F2-S1 price chart. Unlike {@code /indicators}, {@code limit}
     * is bounded 1-{@value #MAX_LIMIT} (not 34-1000) — a chart with fewer than 34 candles still renders, just with
     * an empty indicator series.
     */
    @GetMapping("/{symbol}/chart-data")
    public ChartDataResponse chartData(@PathVariable String symbol,
                                        @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidIndicatorRequestException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        return indicatorService.getChartData(symbol, limit);
    }
}
