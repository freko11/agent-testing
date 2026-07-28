package com.autotrade.dashboard.marketdata;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Hardcoded NYSE/NASDAQ regular-hours calendar (09:30-16:00 America/New_York, Mon-Fri).
 * Holiday and early-close awareness are out of scope for v1 — flagged, not silently ignored.
 */
@Component
public class MarketHoursService {

    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final Clock clock;

    public MarketHoursService(Clock clock) {
        this.clock = clock;
    }

    public boolean isRegularMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(MARKET_ZONE);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
    }
}
