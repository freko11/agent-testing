package com.autotrade.dashboard.tradingmode;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialRepository;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderRepository;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.order.OrderStatus;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * current()/switchTo() behavior for E6-F1-S1's global paper/live switch and E6-F1-S2's paper-trade threshold
 * gate, against a real (H2, Oracle-mode) datasource. The threshold is overridden to 3 so tests don't need to
 * seed dozens of fixture orders.
 */
@SpringBootTest
@TestPropertySource(properties = "trading-mode.paper-trade-threshold=3")
@Transactional
class TradingModeServiceTest {

    @Autowired
    private TradingModeService tradingModeService;
    @Autowired
    private TradingModeEventRepository repository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private TickerRepository tickerRepository;
    @Autowired
    private BrokerCredentialRepository brokerCredentialRepository;

    private Ticker ticker;
    private BrokerCredential credential;

    private void seedFixtures() {
        ticker = tickerRepository.saveAndFlush(new Ticker("TMTEST" + UUID.randomUUID().toString().substring(0, 8),
                AssetType.STOCK, "NASDAQ"));
        credential = brokerCredentialRepository.saveAndFlush(
                new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "key", "secret"));
    }

    private void seedOrder(TradingMode mode, OrderStatus status) {
        if (ticker == null) {
            seedFixtures();
        }
        Order order = new Order(ticker, credential, Broker.ALPACA, AssetType.STOCK, OrderSide.BUY,
                BigDecimal.ONE, new BigDecimal("200"), new BigDecimal("180"), "co-" + UUID.randomUUID());
        order.setOrderMode(mode);
        order.setStatus(status);
        orderRepository.saveAndFlush(order);
    }

    @Test
    void current_noHistory_defaultsToPaper() {
        assertEquals(TradingMode.PAPER, tradingModeService.current());
        assertNull(tradingModeService.currentState().changedAt());
    }

    @Test
    void switchTo_live_belowThreshold_throwsPaperTradeThresholdNotMetException_noHistoryPersisted() {
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);

        PaperTradeThresholdNotMetException ex = assertThrows(PaperTradeThresholdNotMetException.class,
                () -> tradingModeService.switchTo(TradingMode.LIVE));

        assertEquals(2, ex.getCompleted());
        assertEquals(3, ex.getRequired());
        assertEquals(TradingMode.PAPER, tradingModeService.current());
        assertEquals(0, repository.count());
    }

    @Test
    void switchTo_live_atThreshold_succeeds_insertsNewRow() {
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);

        TradingModeResponse response = tradingModeService.switchTo(TradingMode.LIVE);

        assertEquals(TradingMode.LIVE, response.mode());
        assertEquals(1, repository.count());
    }

    @Test
    void switchTo_live_excludesNonFilledAndNonPaperOrders() {
        seedOrder(TradingMode.PAPER, OrderStatus.REJECTED);
        seedOrder(TradingMode.PAPER, OrderStatus.CANCELLED);
        seedOrder(TradingMode.PAPER, OrderStatus.FAILED);
        seedOrder(TradingMode.PAPER, OrderStatus.SUBMISSION_UNKNOWN);
        seedOrder(TradingMode.PAPER, OrderStatus.PARTIALLY_PROTECTED);
        seedOrder(TradingMode.LIVE, OrderStatus.FILLED);

        assertThrows(PaperTradeThresholdNotMetException.class, () -> tradingModeService.switchTo(TradingMode.LIVE));
        assertEquals(0, tradingModeService.currentState().successfulPaperTrades());
    }

    @Test
    void switchTo_live_thenLive_isIdempotentNoOp_evenBelowThreshold() {
        repository.save(new TradingModeEvent(TradingMode.LIVE));
        assertEquals(TradingMode.LIVE, tradingModeService.current());

        TradingModeResponse response = tradingModeService.switchTo(TradingMode.LIVE);

        assertEquals(TradingMode.LIVE, response.mode());
        assertEquals(1, repository.count());
    }

    @Test
    void switchTo_paper_whenAlreadyPaperByDefault_isNoOp_noRowPersisted() {
        TradingModeResponse response = tradingModeService.switchTo(TradingMode.PAPER);

        assertEquals(TradingMode.PAPER, response.mode());
        assertEquals(0, repository.count());
    }

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

    @Test
    void currentState_reportsProgressFields_correctly() {
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);

        TradingModeResponse state = tradingModeService.currentState();

        assertEquals(1, state.successfulPaperTrades());
        assertEquals(3, state.paperTradeThreshold());
        assertFalse(state.liveModeUnlocked());

        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);
        seedOrder(TradingMode.PAPER, OrderStatus.FILLED);

        assertTrue(tradingModeService.currentState().liveModeUnlocked());
    }
}
