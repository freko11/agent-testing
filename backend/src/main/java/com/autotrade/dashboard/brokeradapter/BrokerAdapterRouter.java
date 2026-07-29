package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.ticker.AssetType;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the right {@link BrokerAdapter} for a ticker's {@link AssetType}
 * (STOCK -&gt; Alpaca, CRYPTO -&gt; Binance) — the routing component both
 * {@code BrokerAdapterConfig} and {@code BinanceFuturesAdapterConfig}
 * deliberately deferred as YAGNI until a second adapter existed to route
 * between. Both {@code @Bean BrokerAdapter}s are already plain {@code
 * BrokerAdapter}-typed (wrapped in {@link RetryingBrokerAdapter}), so a
 * {@code List<BrokerAdapter>} injection picks up both with no {@code
 * @Qualifier} needed — same {@code EnumMap}-keyed-by-{@code
 * supportedAssetType()} pattern {@code MarketDataService} already
 * established for market-data clients.
 */
@Service
public class BrokerAdapterRouter {

    private final Map<AssetType, BrokerAdapter> adaptersByAssetType;

    public BrokerAdapterRouter(List<BrokerAdapter> adapters) {
        this.adaptersByAssetType = new EnumMap<>(AssetType.class);
        for (BrokerAdapter adapter : adapters) {
            adaptersByAssetType.put(adapter.supportedAssetType(), adapter);
        }
    }

    public BrokerAdapter forAssetType(AssetType assetType) {
        BrokerAdapter adapter = adaptersByAssetType.get(assetType);
        if (adapter == null) {
            throw new IllegalStateException("No BrokerAdapter registered for asset type " + assetType);
        }
        return adapter;
    }
}
