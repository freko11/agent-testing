package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.order.OrderService;
import com.autotrade.dashboard.ticker.AssetType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Proves E6-F2-S1's hard per-order leverage/position-size caps bind independently of each other and of the current global trading mode. */
class RiskLimitServiceTest {

    private static final RiskLimitsProperties LIMITS =
            new RiskLimitsProperties(new BigDecimal("5000"), new BigDecimal("5"), new BigDecimal("2000"));

    private final RiskLimitService service = new RiskLimitService(LIMITS);

    @Test
    void stockOrder_atExactPositionSizeCap_allowed() {
        assertDoesNotThrow(() -> service.enforcePerOrderCaps(AssetType.STOCK, new BigDecimal("5000"), BigDecimal.ONE));
    }

    @Test
    void stockOrder_overPositionSizeCap_throwsRiskLimitExceeded() {
        assertThrows(RiskLimitExceededException.class,
                () -> service.enforcePerOrderCaps(AssetType.STOCK, new BigDecimal("5000.01"), BigDecimal.ONE));
    }

    @Test
    void cryptoOrder_leverageAtExactCap_allowed() {
        assertDoesNotThrow(() -> service.enforcePerOrderCaps(AssetType.CRYPTO, new BigDecimal("100"), new BigDecimal("5")));
    }

    @Test
    void cryptoOrder_leverageOverConfiguredCap_throwsRiskLimitExceeded_evenWithinAdapterCeiling() {
        // 10x is within OrderService.MAX_CRYPTO_LEVERAGE (20x) but above this configured cap (5x) —
        // proves this is a stricter, independently-configured risk cap, not a re-test of the adapter bound.
        assertThrows(RiskLimitExceededException.class,
                () -> service.enforcePerOrderCaps(AssetType.CRYPTO, new BigDecimal("100"), new BigDecimal("10")));
    }

    @Test
    void cryptoOrder_notionalOverCap_throwsRiskLimitExceeded_evenWhenLeverageWithinCap() {
        // leverage (1x) is well within cap, but amountUsd * leverage = 3000 > 2000 cap.
        assertThrows(RiskLimitExceededException.class,
                () -> service.enforcePerOrderCaps(AssetType.CRYPTO, new BigDecimal("3000"), BigDecimal.ONE));
    }

    @Test
    void cryptoOrder_withinAllCaps_allowed() {
        assertDoesNotThrow(() -> service.enforcePerOrderCaps(AssetType.CRYPTO, new BigDecimal("400"), new BigDecimal("5")));
    }

    @Test
    void cryptoMaxLeverageConfiguredAboveAdapterCeiling_failsFastAtConstruction() {
        RiskLimitsProperties invalid = new RiskLimitsProperties(new BigDecimal("5000"),
                BigDecimal.valueOf(OrderService.MAX_CRYPTO_LEVERAGE + 1), new BigDecimal("2000"));
        assertThrows(IllegalStateException.class, () -> new RiskLimitService(invalid));
    }
}
