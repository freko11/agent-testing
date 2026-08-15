package com.autotrade.dashboard.order;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.broker.BrokerCredential;
import com.autotrade.dashboard.common.TradingMode;
import com.autotrade.dashboard.indicator.IndicatorSnapshot;
import com.autotrade.dashboard.signal.HoldTerm;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.ticker.AssetType;
import com.autotrade.dashboard.ticker.Ticker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderCsvExporterTest {

    private Ticker ticker() {
        return new Ticker("BTCUSDT", AssetType.CRYPTO, null);
    }

    private BrokerCredential credential() {
        return new BrokerCredential(Broker.BINANCE, TradingMode.PAPER, "enc-key", "enc-secret");
    }

    private Order order(Ticker ticker, String clientOrderId, String rejectionReason) {
        Order order = new Order(ticker, credential(), Broker.BINANCE, ticker.getAssetType(), OrderSide.BUY,
                new BigDecimal("1.00000000"), new BigDecimal("110"), new BigDecimal("90"), clientOrderId);
        order.setOrderMode(TradingMode.PAPER);
        order.setRequestedAmountUsd(new BigDecimal("100"));
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(rejectionReason);
        ReflectionTestUtils.setField(order, "id", 1L);
        ReflectionTestUtils.setField(order, "createdAt", Instant.parse("2026-07-05T12:00:00Z"));
        return order;
    }

    @Test
    void export_emptyOrderList_returnsHeaderRowOnly() {
        String csv = OrderCsvExporter.export(List.of(), Map.of());

        assertEquals("Order ID,Created At (UTC),Ticker,Asset Type,Broker,Mode,Side,Quantity,Requested Amount (USD),"
                + "Leverage,Entry Order Type,Entry Price,Take-Profit Price,Stop-Loss Price,Status,Rejection Reason,"
                + "Client Order ID,Broker Order ID,Submitted At (UTC),Filled At (UTC),Indicator Snapshot ID,"
                + "Signal Call,Matched Rule,Rule Table Version,Suggested Hold-Term\r\n", csv);
    }

    @Test
    void export_orderWithNoSignalSnapshot_leavesSignalColumnsBlank() {
        Order order = order(ticker(), "client-1", null);

        String csv = OrderCsvExporter.export(List.of(order), Map.of());
        String[] lines = csv.split("\r\n");

        assertEquals(2, lines.length);
        assertTrue(lines[1].endsWith(",,,,"), "last four signal columns (snapshot id/call/rule/version) should be blank, hold-term too: " + lines[1]);
    }

    @Test
    void export_orderWithSignalSnapshot_includesMatchedRuleAndHoldTerm() {
        Ticker ticker = ticker();
        IndicatorSnapshot snapshot = new IndicatorSnapshot(ticker, Instant.parse("2026-07-05T00:00:00Z"),
                new BigDecimal("100"), Broker.BINANCE);
        ReflectionTestUtils.setField(snapshot, "id", 42L);
        HoldTerm holdTerm = new HoldTerm(3, 10, "3-10 days", "rationale", "v1");
        SignalCallEntry signalCallEntry = new SignalCallEntry(ticker, snapshot, SignalRuleId.BULLISH_MAJORITY, holdTerm);

        Order order = order(ticker, "client-2", null);
        order.setIndicatorSnapshot(snapshot);

        String csv = OrderCsvExporter.export(List.of(order), Map.of(42L, signalCallEntry));

        assertTrue(csv.contains(",42,BUY,BULLISH_MAJORITY,v6,3-10 days\r\n"), csv);
    }

    @Test
    void export_rejectionReasonWithComma_isQuoted() {
        Order order = order(ticker(), "client-3", "Insufficient margin, retry later");

        String csv = OrderCsvExporter.export(List.of(order), Map.of());

        assertTrue(csv.contains("\"Insufficient margin, retry later\""), csv);
    }

    @Test
    void export_rejectionReasonWithQuote_isEscapedByDoubling() {
        Order order = order(ticker(), "client-4", "Broker said \"no\"");

        String csv = OrderCsvExporter.export(List.of(order), Map.of());

        assertTrue(csv.contains("\"Broker said \"\"no\"\"\""), csv);
    }
}
