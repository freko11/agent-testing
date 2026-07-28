package com.autotrade.dashboard.watchlist;

import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.TickerNotRegisteredException;
import com.autotrade.dashboard.ticker.TickerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** add/remove/list behavior for E3-F3-S1's watchlist, against a real (H2, Oracle-mode) datasource. */
@SpringBootTest
@Transactional
class WatchlistServiceTest {

    @Autowired
    private WatchlistService watchlistService;
    @Autowired
    private TickerService tickerService;

    @Test
    void add_registeredTicker_savesEntry() {
        tickerService.resolveOrRegister("AAPL", AssetType.STOCK, "NASDAQ");

        WatchlistEntry entry = watchlistService.add("aapl");

        assertEquals("AAPL", entry.getTicker().getSymbol());
        assertTrue(watchlistService.contains("AAPL"));
    }

    @Test
    void add_unregisteredTicker_throwsNotRegistered() {
        assertThrows(TickerNotRegisteredException.class, () -> watchlistService.add("NOPE"));
    }

    @Test
    void add_alreadyWatchlisted_isIdempotent() {
        tickerService.resolveOrRegister("MSFT", AssetType.STOCK, "NASDAQ");

        WatchlistEntry first = watchlistService.add("MSFT");
        WatchlistEntry second = watchlistService.add("MSFT");

        assertEquals(first.getId(), second.getId());
        assertEquals(1, watchlistService.list().size());
    }

    @Test
    void remove_watchlistedTicker_deletesEntry() {
        tickerService.resolveOrRegister("ETHUSDT", AssetType.CRYPTO, null);
        watchlistService.add("ETHUSDT");

        watchlistService.remove("ETHUSDT");

        assertFalse(watchlistService.contains("ETHUSDT"));
    }

    @Test
    void remove_notWatchlisted_isNoOp() {
        tickerService.resolveOrRegister("SOLUSDT", AssetType.CRYPTO, null);

        watchlistService.remove("SOLUSDT");

        assertFalse(watchlistService.contains("SOLUSDT"));
    }

    @Test
    void remove_unregisteredTicker_throwsNotRegistered() {
        assertThrows(TickerNotRegisteredException.class, () -> watchlistService.remove("NOPE"));
    }

    @Test
    void list_ordersByMostRecentlyAddedFirst() {
        tickerService.resolveOrRegister("DOGEUSDT", AssetType.CRYPTO, null);
        tickerService.resolveOrRegister("ADAUSDT", AssetType.CRYPTO, null);
        watchlistService.add("DOGEUSDT");
        watchlistService.add("ADAUSDT");

        List<WatchlistEntry> entries = watchlistService.list();

        assertEquals("ADAUSDT", entries.get(0).getTicker().getSymbol());
        assertEquals("DOGEUSDT", entries.get(1).getTicker().getSymbol());
    }
}
