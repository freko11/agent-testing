package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Proves the router picks the adapter matching each asset type, and fails loudly on a wiring gap rather than silently misrouting a trade. */
class BrokerAdapterRouterTest {

    private final MockBrokerAdapter alpaca = new MockBrokerAdapter(Broker.ALPACA, AssetType.STOCK);
    private final MockBrokerAdapter binance = new MockBrokerAdapter(Broker.BINANCE, AssetType.CRYPTO);

    @Test
    void routesStockToAlpacaAndCryptoToBinance() {
        BrokerAdapterRouter router = new BrokerAdapterRouter(List.of(alpaca, binance));

        assertEquals(alpaca, router.forAssetType(AssetType.STOCK));
        assertEquals(binance, router.forAssetType(AssetType.CRYPTO));
    }

    @Test
    void missingAdapterForAssetType_throwsIllegalStateException() {
        BrokerAdapterRouter router = new BrokerAdapterRouter(List.of(alpaca));

        assertThrows(IllegalStateException.class, () -> router.forAssetType(AssetType.CRYPTO));
    }
}
