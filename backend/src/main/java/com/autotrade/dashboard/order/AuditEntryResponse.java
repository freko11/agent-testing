package com.autotrade.dashboard.order;

import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalRuleId;
import com.autotrade.dashboard.ticker.AssetType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single audit-trail row (E6-F3-S3) -- an order's resolved outcome alongside the frozen signal
 * snapshot that triggered it (E6-F3-S1/S2's {@link OrderAuditEntry}), for the dashboard's
 * audit-trail viewer. Deliberately a separate DTO from {@link OrderResponse}: this view is about
 * "why did this order fire," not "what is this order's current status" -- {@code
 * indicatorSnapshot}'s raw RSI/MACD values are left out as backtest/calibration-tool territory,
 * out of proportion for a review row.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEntryResponse(
        Long id,
        String tickerSymbol,
        AssetType assetType,
        OrderSide side,
        SignalCall call,
        SignalRuleId matchedRule,
        String matchedRuleRationale,
        String ruleTableVersion,
        Integer holdTermMinDays,
        Integer holdTermMaxDays,
        OrderStatus resultStatus,
        String rejectionReason,
        BigDecimal entryPrice,
        Instant loggedAt) {

    static AuditEntryResponse from(OrderAuditEntry entry) {
        SignalCallEntry signalCallEntry = entry.getSignalCallEntry();
        return new AuditEntryResponse(entry.getId(), entry.getTicker().getSymbol(), entry.getOrder().getAssetType(),
                entry.getOrder().getSide(), signalCallEntry.getCall(), signalCallEntry.getMatchedRule(),
                signalCallEntry.getMatchedRule().rationale(), entry.getRuleTableVersion(),
                signalCallEntry.getHoldTermMinDays(), signalCallEntry.getHoldTermMaxDays(), entry.getResultStatus(),
                entry.getRejectionReason(), entry.getEntryPrice(), entry.getLoggedAt());
    }
}
