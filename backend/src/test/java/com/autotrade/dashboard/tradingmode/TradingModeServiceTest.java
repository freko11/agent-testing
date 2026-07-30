package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.common.TradingMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** current()/switchTo() behavior for E6-F1-S1's global paper/live switch, against a real (H2, Oracle-mode) datasource. */
@SpringBootTest
@Transactional
class TradingModeServiceTest {

    @Autowired
    private TradingModeService tradingModeService;
    @Autowired
    private TradingModeEventRepository repository;

    @Test
    void current_noHistory_defaultsToPaper() {
        assertEquals(TradingMode.PAPER, tradingModeService.current());
        assertNull(tradingModeService.currentState().changedAt());
    }

    @Test
    void switchTo_live_alwaysThrows_noHistoryPersisted() {
        assertThrows(LiveModeNotYetAvailableException.class, () -> tradingModeService.switchTo(TradingMode.LIVE));

        assertEquals(TradingMode.PAPER, tradingModeService.current());
        assertEquals(0, repository.count());
    }

    @Test
    void switchTo_live_fromAnExplicitlySeededPaperRow_stillThrows_noNewRowPersisted() {
        repository.save(new TradingModeEvent(TradingMode.PAPER));

        assertThrows(LiveModeNotYetAvailableException.class, () -> tradingModeService.switchTo(TradingMode.LIVE));

        assertEquals(1, repository.count());
    }

    @Test
    void switchTo_paper_whenAlreadyPaperByDefault_isNoOp_noRowPersisted() {
        TradingModeResponse response = tradingModeService.switchTo(TradingMode.PAPER);

        assertEquals(TradingMode.PAPER, response.mode());
        assertEquals(0, repository.count());
    }

    /**
     * {@code switchTo} can never itself produce a {@code LIVE} row today (it unconditionally throws — see {@link
     * LiveModeNotYetAvailableException}), so this seeds one directly via the repository to exercise the
     * "switch away from a non-default mode" path ahead of E6-F1-S2/S3 making it reachable for real.
     */
    @Test
    void switchTo_paper_fromADirectlySeededLiveRow_succeeds_insertsNewRow() {
        repository.save(new TradingModeEvent(TradingMode.LIVE));
        assertEquals(TradingMode.LIVE, tradingModeService.current());

        TradingModeResponse response = tradingModeService.switchTo(TradingMode.PAPER);

        assertEquals(TradingMode.PAPER, response.mode());
        assertEquals(TradingMode.PAPER, tradingModeService.current());
        assertEquals(2, repository.count());
    }

    @Test
    void current_readsTheLatestOfMultipleSeededRows() {
        repository.save(new TradingModeEvent(TradingMode.PAPER));
        repository.save(new TradingModeEvent(TradingMode.LIVE));
        repository.save(new TradingModeEvent(TradingMode.PAPER));

        assertEquals(TradingMode.PAPER, tradingModeService.current());
    }
}
