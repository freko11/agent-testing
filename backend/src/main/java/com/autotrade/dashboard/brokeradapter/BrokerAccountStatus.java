package com.autotrade.dashboard.brokeradapter;

import com.autotrade.dashboard.broker.Broker;
import com.autotrade.dashboard.common.TradingMode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Account balance/status for one broker under one {@link TradingMode}, as
 * reported by {@link BrokerAdapter#getAccountStatus}. A {@code balances}
 * list (rather than one cash figure) fits both Alpaca's single-currency cash
 * account and Binance's multi-asset balances without a shape change.
 * {@code equity}/{@code buyingPower} are nullable — Alpaca-shaped concepts
 * that a multi-asset exchange adapter may leave null.
 */
public record BrokerAccountStatus(
        Broker broker,
        TradingMode tradingMode,
        List<AssetBalance> balances,
        BigDecimal equity,
        BigDecimal buyingPower,
        Instant asOf) {

    public record AssetBalance(String asset, BigDecimal free, BigDecimal locked) {
    }
}
