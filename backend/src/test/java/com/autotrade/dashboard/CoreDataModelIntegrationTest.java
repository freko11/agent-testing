package com.autotrade.dashboard;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.broker.BrokerCredentialRepository;
import com.autotrade.dashboard.broker.BrokerCredentialService;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.indicator.IndicatorSnapshotRepository;
import com.autotrade.dashboard.order.Order;
import com.autotrade.dashboard.order.OrderRepository;
import com.autotrade.dashboard.order.OrderSide;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.ticker.TickerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for E1-F2-S1/S2/S3's core data model: proves the four
 * repositories round-trip through a real (H2, Oracle-compatibility-mode)
 * datasource with Flyway-applied schema, and that the DB-level constraints
 * from V1__init_core_schema.sql actually hold, not just the JPA annotations.
 *
 * Plain {@code @SpringBootTest} (not {@code @DataJpaTest}) is used deliberately
 * so Spring wires the real configured datasource (H2 in Oracle-compatibility
 * mode, per src/test/resources/application.properties) instead of
 * {@code @DataJpaTest}'s default embedded-datasource auto-replacement.
 */
@SpringBootTest
@Transactional
class CoreDataModelIntegrationTest {

    @Autowired
    private TickerRepository tickerRepository;

    @Autowired
    private IndicatorSnapshotRepository indicatorSnapshotRepository;

    @Autowired
    private BrokerCredentialRepository brokerCredentialRepository;

    @Autowired
    private BrokerCredentialService brokerCredentialService;

    @Autowired
    private OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void crudRoundTripThroughAllFourRepositories() {
        // --- Ticker: insert, fetch, update ---
        Ticker ticker = tickerRepository.saveAndFlush(new Ticker("AAPL", AssetType.STOCK, "NASDAQ"));
        assertNotNull(ticker.getId());
        assertNotNull(ticker.getCreatedAt());

        Ticker fetchedTicker = tickerRepository.findById(ticker.getId()).orElseThrow();
        assertEquals("AAPL", fetchedTicker.getSymbol());

        fetchedTicker.setExchange("NYSE");
        tickerRepository.saveAndFlush(fetchedTicker);
        assertEquals("NYSE", tickerRepository.findById(ticker.getId()).orElseThrow().getExchange());

        // --- IndicatorSnapshot: insert, fetch, update, named finder ---
        Instant snapshotAt = Instant.now().minus(1, ChronoUnit.HOURS);
        IndicatorSnapshot snapshot = new IndicatorSnapshot(
                fetchedTicker, snapshotAt, new BigDecimal("189.50000000"), Broker.ALPACA);
        snapshot.setRsi(new BigDecimal("55.1234"));
        snapshot = indicatorSnapshotRepository.saveAndFlush(snapshot);
        assertNotNull(snapshot.getId());

        IndicatorSnapshot fetchedSnapshot = indicatorSnapshotRepository.findById(snapshot.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("189.50000000").compareTo(fetchedSnapshot.getPrice()));

        fetchedSnapshot.setRsi(new BigDecimal("61.0000"));
        indicatorSnapshotRepository.saveAndFlush(fetchedSnapshot);
        assertEquals(0, new BigDecimal("61.0000")
                .compareTo(indicatorSnapshotRepository.findById(snapshot.getId()).orElseThrow().getRsi()));

        Optional<IndicatorSnapshot> latest =
                indicatorSnapshotRepository.findFirstByTickerIdOrderBySnapshotAtDesc(fetchedTicker.getId());
        assertTrue(latest.isPresent());
        assertEquals(snapshot.getId(), latest.get().getId());

        // --- BrokerCredential: insert (encrypted round trip via the service), fetch, update, named finder ---
        BrokerCredential credential = brokerCredentialService.store(
                Broker.ALPACA, TradingMode.PAPER, "plaintext-api-key", "plaintext-api-secret");
        brokerCredentialRepository.flush();
        assertNotNull(credential.getId());
        assertNotNull(credential.getCreatedAt());
        Instant firstUpdatedAt = credential.getUpdatedAt();

        BrokerCredential fetchedCredential = brokerCredentialRepository.findById(credential.getId()).orElseThrow();
        // The stored columns are ciphertext, not plaintext — only the service can recover the plaintext.
        assertNotEquals("plaintext-api-key", fetchedCredential.getApiKeyCiphertext());
        BrokerCredentialService.DecryptedCredential decrypted =
                brokerCredentialService.readDecrypted(fetchedCredential);
        assertEquals("plaintext-api-key", decrypted.apiKey());
        assertEquals("plaintext-api-secret", decrypted.apiSecret());

        fetchedCredential.setActive(false);
        brokerCredentialRepository.saveAndFlush(fetchedCredential);
        BrokerCredential reFetchedCredential = brokerCredentialRepository.findById(credential.getId()).orElseThrow();
        assertFalse(reFetchedCredential.isActive());
        assertTrue(!reFetchedCredential.getUpdatedAt().isBefore(firstUpdatedAt));

        // Re-activate so the named finder (which filters on isActive=true) can find it.
        reFetchedCredential.setActive(true);
        brokerCredentialRepository.saveAndFlush(reFetchedCredential);
        Optional<BrokerCredential> activeCredential = brokerCredentialRepository
                .findByBrokerAndEnvironmentAndIsActiveTrue(Broker.ALPACA, TradingMode.PAPER);
        assertTrue(activeCredential.isPresent());
        assertEquals(credential.getId(), activeCredential.get().getId());

        // --- Order: insert, fetch, update, named finders ---
        String clientOrderId = "co-" + UUID.randomUUID();
        Order order = new Order(
                fetchedTicker, reFetchedCredential, Broker.ALPACA, AssetType.STOCK, OrderSide.BUY,
                new BigDecimal("10.00000000"), new BigDecimal("200.00000000"), new BigDecimal("180.00000000"),
                clientOrderId);
        order.setIndicatorSnapshot(snapshot);
        order = orderRepository.saveAndFlush(order);
        assertNotNull(order.getId());
        Instant orderFirstUpdatedAt = order.getUpdatedAt();

        Order fetchedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(clientOrderId, fetchedOrder.getClientOrderId());
        assertEquals(0, new BigDecimal("1.00").compareTo(fetchedOrder.getLeverage()));

        fetchedOrder.setStatus(com.autotrade.dashboard.order.OrderStatus.SUBMITTED);
        fetchedOrder.setSubmittedAt(Instant.now());
        orderRepository.saveAndFlush(fetchedOrder);
        Order reFetchedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(com.autotrade.dashboard.order.OrderStatus.SUBMITTED, reFetchedOrder.getStatus());
        assertTrue(!reFetchedOrder.getUpdatedAt().isBefore(orderFirstUpdatedAt));

        assertEquals(Optional.of(order.getId()),
                orderRepository.findByClientOrderId(clientOrderId).map(Order::getId));

        Long orderId = order.getId();
        List<Order> byModeAndWindow = orderRepository.findByOrderModeAndCreatedAtBetweenOrderByCreatedAtAsc(
                TradingMode.PAPER, order.getCreatedAt().minusSeconds(60), order.getCreatedAt().plusSeconds(60));
        assertTrue(byModeAndWindow.stream().anyMatch(o -> o.getId().equals(orderId)));

        // --- Delete: respect FK dependency order (order -> snapshot/credential/ticker) ---
        orderRepository.delete(reFetchedOrder);
        orderRepository.flush();
        assertTrue(orderRepository.findById(order.getId()).isEmpty());

        indicatorSnapshotRepository.delete(fetchedSnapshot);
        indicatorSnapshotRepository.flush();
        assertTrue(indicatorSnapshotRepository.findById(snapshot.getId()).isEmpty());

        brokerCredentialRepository.delete(reFetchedCredential);
        brokerCredentialRepository.flush();
        assertTrue(brokerCredentialRepository.findById(credential.getId()).isEmpty());

        tickerRepository.delete(fetchedTicker);
        tickerRepository.flush();
        assertTrue(tickerRepository.findById(ticker.getId()).isEmpty());
    }

    @Test
    void foreignKeyViolation_orderAgainstNonExistentTicker() {
        BrokerCredential credential = brokerCredentialRepository.saveAndFlush(
                new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "key", "secret"));

        // A reference to a ticker id that has never been persisted — Hibernate defers the
        // existence check to the DB, which is exactly what we want to exercise here.
        Ticker nonExistentTicker = entityManager.getReference(Ticker.class, Long.MAX_VALUE);

        Order order = new Order(
                nonExistentTicker, credential, Broker.ALPACA, AssetType.STOCK, OrderSide.BUY,
                BigDecimal.TEN, new BigDecimal("200"), new BigDecimal("180"), "co-" + UUID.randomUUID());

        assertThrows(DataIntegrityViolationException.class, () -> orderRepository.saveAndFlush(order));
    }

    @Test
    void uniqueViolation_duplicateTickerSymbol() {
        tickerRepository.saveAndFlush(new Ticker("DUPTEST", AssetType.STOCK, "NASDAQ"));
        Ticker duplicate = new Ticker("DUPTEST", AssetType.CRYPTO, null);

        assertThrows(DataIntegrityViolationException.class, () -> tickerRepository.saveAndFlush(duplicate));
    }

    @Test
    void uniqueViolation_duplicateClientOrderId() {
        Ticker ticker = tickerRepository.saveAndFlush(new Ticker("DUPCID", AssetType.STOCK, "NASDAQ"));
        BrokerCredential credential = brokerCredentialRepository.saveAndFlush(
                new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "key", "secret"));
        String sharedClientOrderId = "co-" + UUID.randomUUID();

        orderRepository.saveAndFlush(new Order(
                ticker, credential, Broker.ALPACA, AssetType.STOCK, OrderSide.BUY,
                BigDecimal.ONE, new BigDecimal("200"), new BigDecimal("180"), sharedClientOrderId));

        Order secondOrderSameClientId = new Order(
                ticker, credential, Broker.ALPACA, AssetType.STOCK, OrderSide.SELL,
                BigDecimal.ONE, new BigDecimal("210"), new BigDecimal("190"), sharedClientOrderId);

        assertThrows(DataIntegrityViolationException.class,
                () -> orderRepository.saveAndFlush(secondOrderSameClientId));
    }

    @Test
    void checkConstraintViolation_stockOrderCannotCarryLeverage() {
        Ticker ticker = tickerRepository.saveAndFlush(new Ticker("STKLEV", AssetType.STOCK, "NASDAQ"));
        BrokerCredential credential = brokerCredentialRepository.saveAndFlush(
                new BrokerCredential(Broker.ALPACA, TradingMode.PAPER, "key", "secret"));

        Order stockOrderWithLeverage = new Order(
                ticker, credential, Broker.ALPACA, AssetType.STOCK, OrderSide.BUY,
                BigDecimal.ONE, new BigDecimal("200"), new BigDecimal("180"), "co-" + UUID.randomUUID());
        stockOrderWithLeverage.setLeverage(new BigDecimal("2.00"));

        assertThrows(DataIntegrityViolationException.class,
                () -> orderRepository.saveAndFlush(stockOrderWithLeverage));
    }
}
