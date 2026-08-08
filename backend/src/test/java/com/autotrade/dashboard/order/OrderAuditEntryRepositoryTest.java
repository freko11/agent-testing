package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialRepository;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.IndicatorSnapshotRepository;
import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real (H2, Oracle-compatibility-mode) round trip for {@link
 * OrderAuditEntryRepository#findAllByOrderByLoggedAtDesc} (E6-F3-S3) — the codebase's first
 * genuinely paginated query (explicit {@code countQuery} alongside a {@code JOIN FETCH}-bearing
 * main query), so a mock-based unit test can't prove the JPQL itself is valid or that pagination
 * math (total count, ordering) actually works against a real datasource. Plain
 * {@code @SpringBootTest}, not {@code @DataJpaTest}, matching {@code
 * CoreDataModelIntegrationTest}'s precedent for the same reason (real configured datasource, not
 * an auto-replaced embedded one).
 */
@SpringBootTest
@Transactional
class OrderAuditEntryRepositoryTest {

    @Autowired
    private TickerRepository tickerRepository;
    @Autowired
    private BrokerCredentialRepository brokerCredentialRepository;
    @Autowired
    private IndicatorSnapshotRepository indicatorSnapshotRepository;
    @Autowired
    private SignalCallEntryRepository signalCallEntryRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderAuditEntryRepository orderAuditEntryRepository;

    private BrokerCredential sharedCredential;

    private OrderAuditEntry persistAuditEntry(String tickerSymbol, OrderStatus resultStatus) {
        Ticker ticker = tickerRepository.saveAndFlush(new Ticker(tickerSymbol, AssetType.CRYPTO, null));
        // broker_credentials has a unique (broker, environment) constraint -- one shared row per test,
        // not one per audit entry, matching how a real account only ever has one active credential per pair.
        if (sharedCredential == null) {
            sharedCredential = brokerCredentialRepository.saveAndFlush(
                    new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "key", "secret"));
        }
        BrokerCredential credential = sharedCredential;
        IndicatorSnapshot snapshot = indicatorSnapshotRepository.saveAndFlush(
                new IndicatorSnapshot(ticker, Instant.now(), new BigDecimal("100.00000000"), Broker.BINANCE));
        HoldTerm holdTerm = new HoldTerm(1, 5, "1-5 days", "rationale", "v1");
        SignalCallEntry signalCallEntry = signalCallEntryRepository.saveAndFlush(
                new SignalCallEntry(ticker, snapshot, SignalRuleId.BULLISH_MAJORITY, holdTerm));
        Order order = orderRepository.saveAndFlush(new Order(ticker, credential, Broker.BINANCE, AssetType.CRYPTO,
                OrderSide.BUY, BigDecimal.ONE, new BigDecimal("110"), new BigDecimal("90"), "co-" + UUID.randomUUID()));
        return orderAuditEntryRepository.saveAndFlush(new OrderAuditEntry(order, signalCallEntry,
                signalCallEntry.getRuleTableVersion(), resultStatus, null, null, new BigDecimal("100.5")));
    }

    @Test
    void findAllByOrderByLoggedAtDesc_ordersNewestFirstAndFetchesAssociationsEagerly() throws InterruptedException {
        OrderAuditEntry first = persistAuditEntry("BTCUSDT", OrderStatus.FILLED);
        Thread.sleep(5); // loggedAt has no sub-millisecond guarantee across two @PrePersist calls otherwise
        OrderAuditEntry second = persistAuditEntry("DOGEUSDT", OrderStatus.REJECTED);

        Page<OrderAuditEntry> page = orderAuditEntryRepository.findAllByOrderByLoggedAtDesc(PageRequest.of(0, 25));

        assertEquals(2, page.getTotalElements());
        assertEquals(1, page.getTotalPages());
        List<OrderAuditEntry> content = page.getContent();
        assertEquals(second.getId(), content.get(0).getId());
        assertEquals(first.getId(), content.get(1).getId());
        // JOIN FETCH proves out here: these associations are readable without a LazyInitializationException.
        assertEquals("DOGEUSDT", content.get(0).getTicker().getSymbol());
        assertEquals(OrderSide.BUY, content.get(0).getOrder().getSide());
        assertEquals(SignalRuleId.BULLISH_MAJORITY, content.get(0).getSignalCallEntry().getMatchedRule());
    }

    @Test
    void findAllByOrderByLoggedAtDesc_pagination_splitsAcrossPagesWithCorrectTotals() {
        persistAuditEntry("BTCUSDT", OrderStatus.FILLED);
        persistAuditEntry("DOGEUSDT", OrderStatus.FILLED);
        persistAuditEntry("SOLUSDT", OrderStatus.FILLED);

        Page<OrderAuditEntry> firstPage = orderAuditEntryRepository.findAllByOrderByLoggedAtDesc(PageRequest.of(0, 2));
        Page<OrderAuditEntry> secondPage = orderAuditEntryRepository.findAllByOrderByLoggedAtDesc(PageRequest.of(1, 2));

        assertEquals(3, firstPage.getTotalElements());
        assertEquals(2, firstPage.getTotalPages());
        assertEquals(2, firstPage.getContent().size());
        assertEquals(1, secondPage.getContent().size());
        assertTrue(firstPage.hasNext());
        assertTrue(secondPage.isLast());
    }
}
