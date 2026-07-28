package com.autotrade.dashboard.ticker;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** find-or-create / conflict / normalization behavior for E2-F1-S1's ticker registration, against a real (H2, Oracle-mode) datasource. */
@SpringBootTest
@Transactional
class TickerServiceTest {

    @Autowired
    private TickerService tickerService;

    @Test
    void resolveOrRegister_newSymbol_createsAndNormalizesCase() {
        Ticker ticker = tickerService.resolveOrRegister("aapl", AssetType.STOCK, "NASDAQ");

        assertEquals("AAPL", ticker.getSymbol());
        assertTrue(tickerService.findRegistered("AAPL").isPresent());
        assertTrue(tickerService.findRegistered("aapl").isPresent());
    }

    @Test
    void resolveOrRegister_sameSymbolSameAssetType_isIdempotent() {
        Ticker first = tickerService.resolveOrRegister("MSFT", AssetType.STOCK, "NASDAQ");
        Ticker second = tickerService.resolveOrRegister("MSFT", AssetType.STOCK, "NASDAQ");

        assertEquals(first.getId(), second.getId());
    }

    @Test
    void resolveOrRegister_sameSymbolDifferentAssetType_throwsConflict() {
        tickerService.resolveOrRegister("ETHUSDT", AssetType.CRYPTO, null);

        assertThrows(TickerAssetTypeConflictException.class,
                () -> tickerService.resolveOrRegister("ETHUSDT", AssetType.STOCK, null));
    }

    @Test
    void findRegistered_unknownSymbol_returnsEmpty() {
        Optional<Ticker> result = tickerService.findRegistered("NOPE");

        assertTrue(result.isEmpty());
    }
}
