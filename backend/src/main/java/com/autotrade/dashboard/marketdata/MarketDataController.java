package com.autotrade.dashboard.marketdata;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickers")
public class MarketDataController {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/{symbol}/price-history")
    public PriceHistoryResponse priceHistory(@PathVariable String symbol,
                                              @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidPriceHistoryRequestException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        PriceHistoryResult result = marketDataService.getPriceHistory(symbol, limit);
        return PriceHistoryResponse.from(result);
    }
}
