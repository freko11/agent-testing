package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.order.OrderService;
import com.autotrade.dashboard.ticker.AssetType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Hard server-side caps on leverage, per-order position size (E6-F2-S1), and portfolio-level aggregate open
 * exposure (E6-F2-S3) — enforced regardless of what the frontend sent, since {@code frontend/src/trade/validation.ts}
 * is only a UX convenience, not a security boundary. Called from {@code OrderService.submitOrder} as a pre-flight
 * check, before any {@code Order} row is created or the broker is called, alongside the existing shape/bounds checks
 * in {@code OrderService.validate} — this is a separate, independently-configured risk policy layered on top of
 * those, not a replacement for them.
 *
 * <p>"Position size"/"exposure" means notional ({@code amountUsd * leverage}), the actual size of the market
 * position taken — for a stock order leverage is always 1x ({@code OrderService.validate} already enforces that),
 * so notional equals the entered amount.
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
        BigDecimal largestPerOrderCap = limits.stockMaxPositionSizeUsd().max(limits.cryptoMaxPositionSizeUsd());
        if (limits.maxAggregateExposureUsd().compareTo(largestPerOrderCap) < 0) {
            throw new IllegalStateException("risk-limits.max-aggregate-exposure-usd (" + limits.maxAggregateExposureUsd()
                    + ") cannot be smaller than the largest per-order position-size cap ($" + largestPerOrderCap
                    + ") — a single maximally-sized order would then always breach the aggregate cap on its own, "
                    + "even with zero other open exposure.");
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

    /**
     * Portfolio-level cap (E6-F2-S3): rejects a new order if it would push total open exposure — this app's own
     * currently-open orders' notional plus this new order's notional — beyond the configured aggregate ceiling, even
     * when the order is within every per-order limit on its own. {@code currentOpenNotionalUsd} is the caller's
     * responsibility to compute (a DB aggregate, not something this config-only service can derive itself).
     */
    public void enforceAggregateExposureCap(BigDecimal currentOpenNotionalUsd, BigDecimal newOrderNotionalUsd) {
        BigDecimal projectedNotionalUsd = currentOpenNotionalUsd.add(newOrderNotionalUsd);
        if (projectedNotionalUsd.compareTo(limits.maxAggregateExposureUsd()) > 0) {
            throw new RiskLimitExceededException(
                    "Total open exposure $" + projectedNotionalUsd + " (existing $" + currentOpenNotionalUsd
                            + " open + $" + newOrderNotionalUsd + " for this order) would exceed the configured "
                            + "aggregate cap of $" + limits.maxAggregateExposureUsd() + ".");
        }
    }
}
