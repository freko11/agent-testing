package com.autotrade.dashboard.order;

import com.autotrade.dashboard.signal.SignalCallEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Renders order history (E5-F3-S2) as RFC 4180 CSV. Hand-rolled rather than a
 * library, matching this codebase's bias against a dependency for a small,
 * deterministic task (RetryHelper, MarketHoursService, hand-rolled
 * indicators) — one free-text column (rejectionReason) is well short of the
 * complexity that would justify one.
 */
final class OrderCsvExporter {

    private static final String[] HEADER = {
            "Order ID", "Created At (UTC)", "Ticker", "Asset Type", "Broker", "Mode", "Side", "Quantity",
            "Requested Amount (USD)", "Leverage", "Entry Order Type", "Entry Price", "Take-Profit Price",
            "Stop-Loss Price", "Status", "Rejection Reason", "Client Order ID", "Broker Order ID",
            "Submitted At (UTC)", "Filled At (UTC)", "Indicator Snapshot ID", "Signal Call", "Matched Rule",
            "Rule Table Version", "Suggested Hold-Term"
    };

    private OrderCsvExporter() {
    }

    static String export(List<Order> orders, Map<Long, SignalCallEntry> signalCallsBySnapshotId) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, HEADER);
        for (Order order : orders) {
            SignalCallEntry signalCall = order.getIndicatorSnapshot() != null
                    ? signalCallsBySnapshotId.get(order.getIndicatorSnapshot().getId())
                    : null;
            appendRow(csv,
                    String.valueOf(order.getId()),
                    instant(order.getCreatedAt()),
                    order.getTicker().getSymbol(),
                    order.getAssetType().name(),
                    order.getBroker().name(),
                    order.getOrderMode().name(),
                    order.getSide().name(),
                    decimal(order.getQuantity()),
                    decimal(order.getRequestedAmountUsd()),
                    decimal(order.getLeverage()),
                    order.getEntryOrderType().name(),
                    decimal(order.getEntryPrice()),
                    decimal(order.getTakeProfitPrice()),
                    decimal(order.getStopLossPrice()),
                    order.getStatus().name(),
                    order.getRejectionReason() == null ? "" : order.getRejectionReason(),
                    order.getClientOrderId(),
                    order.getBrokerOrderId() == null ? "" : order.getBrokerOrderId(),
                    instant(order.getSubmittedAt()),
                    instant(order.getFilledAt()),
                    order.getIndicatorSnapshot() == null ? "" : String.valueOf(order.getIndicatorSnapshot().getId()),
                    signalCall == null ? "" : signalCall.getCall().name(),
                    signalCall == null ? "" : signalCall.getMatchedRule().name(),
                    signalCall == null ? "" : signalCall.getRuleTableVersion(),
                    holdTerm(signalCall));
        }
        return csv.toString();
    }

    private static String holdTerm(SignalCallEntry signalCall) {
        if (signalCall == null || signalCall.getHoldTermMinDays() == null) {
            return "";
        }
        return signalCall.getHoldTermMinDays() + "-" + signalCall.getHoldTermMaxDays() + " days";
    }

    private static void appendRow(StringBuilder csv, String... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(escape(fields[i]));
        }
        csv.append("\r\n");
    }

    private static String escape(String raw) {
        if (raw.indexOf(',') < 0 && raw.indexOf('"') < 0 && raw.indexOf('\r') < 0 && raw.indexOf('\n') < 0) {
            return raw;
        }
        return '"' + raw.replace("\"", "\"\"") + '"';
    }

    private static String decimal(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String instant(Instant value) {
        return value == null ? "" : DateTimeFormatter.ISO_INSTANT.format(value);
    }
}
