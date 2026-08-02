package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.order.OrderService;
import com.autotrade.dashboard.ticker.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Hard server-side cap on leverage and position size (E6-F2-S1) — enforced regardless of what the frontend sent,
 * since {@code frontend/src/trade/validation.ts} is only a UX convenience, not a security boundary. Called from
 * {@code OrderService.submitOrder} as a pre-flight check, before any {@code Order} row is created or the broker is
 * called, alongside the existing shape/bounds checks in {@code OrderService.validate} — this is a separate,
 * independently-configured risk policy layered on top of those, not a replacement for them.
 *
 * <p>"Position size" means notional exposure ({@code amountUsd * leverage}), the actual size of the market position
 * taken — for a stock order leverage is always 1x ({@code OrderService.validate} already enforces that), so notional
 * equals the entered amount.
 */
@Service
public class RiskLimitService {

    private final RiskLimitsProperties limits;

    public RiskLimitService(RiskLimitsProperties limits) {
        if (limits.cryptoMaxLeverage().compareTo(BigDecimal.valueOf(OrderService.MAX_CRYPTO_LEVERAGE)) > 0) {
            throw new IllegalStateException("risk-limits.crypto-max-leverage (" + limits.cryptoMaxLeverage()
                    + ") cannot exceed the adapter's technical ceiling of " + OrderService.MAX_CRYPTO_LEVERAGE + "x — "
                    + "a cap set above it would never actually bind.");
        }
        this.limits = limits;
    }

    public void enforcePerOrderCaps(AssetType assetType, BigDecimal amountUsd, BigDecimal leverage) {
        BigDecimal notionalUsd = amountUsd.multiply(leverage);

        if (assetType == AssetType.CRYPTO && leverage.compareTo(limits.cryptoMaxLeverage()) > 0) {
            throw new RiskLimitExceededException(
                    "Leverage " + leverage + "x exceeds the configured cap of " + limits.cryptoMaxLeverage() + "x.");
        }

        BigDecimal maxPositionSizeUsd = assetType == AssetType.CRYPTO
                ? limits.cryptoMaxPositionSizeUsd()
                : limits.stockMaxPositionSizeUsd();
        if (notionalUsd.compareTo(maxPositionSizeUsd) > 0) {
            throw new RiskLimitExceededException(
                    "Position size $" + notionalUsd + " exceeds the configured cap of $" + maxPositionSizeUsd + ".");
        }
    }
}
