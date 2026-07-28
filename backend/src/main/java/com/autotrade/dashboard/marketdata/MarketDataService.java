package com.autotrade.dashboard.marketdata;

import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import com.autotrade.dashboard.ticker.TickerService;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {

    private final TickerService tickerService;
    private final Map<AssetType, MarketDataClient> clientsByAssetType;

    public MarketDataService(TickerService tickerService, List<MarketDataClient> clients) {
        this.tickerService = tickerService;
        this.clientsByAssetType = new EnumMap<>(AssetType.class);
        for (MarketDataClient client : clients) {
            clientsByAssetType.put(client.supportedAssetType(), client);
        }
    }

    public PriceHistoryResult getPriceHistory(String symbol, int limit) {
        Ticker ticker = tickerService.findRegistered(symbol)
                .orElseThrow(() -> new TickerNotRegisteredException(symbol));

        MarketDataClient client = clientsByAssetType.get(ticker.getAssetType());
        if (client == null) {
            // Every AssetType has a registered MarketDataClient bean; this indicates a wiring bug, not a runtime/provider fault.
            throw new IllegalStateException("No market data client registered for asset type " + ticker.getAssetType());
        }

        List<Candle> candles = client.fetchRecentCandles(ticker.getSymbol(), limit);
        return new PriceHistoryResult(ticker, client.broker(), candles);
    }
}
