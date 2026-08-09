package com.autotrade.dashboard.marketdata;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Hardcoded NYSE/NASDAQ regular-hours calendar (09:30-16:00 America/New_York, Mon-Fri),
 * plus a hardcoded holiday and early-close (13:00 close) calendar for 2024-2027 — a
 * bounded near-term range, not a computed/algorithmic calendar (no Easter/nth-weekday
 * rules), matching this class's existing "hardcoded, no library" precedent. Dates beyond
 * this range fall back to the plain Mon-Fri/09:30-16:00 calendar with no holiday
 * awareness — flagged here, not silently wrong, since extending the range is a simple
 * data addition when it's next needed.
 */
@Component
public class MarketHoursService {

    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    private static final LocalTime EARLY_CLOSE = LocalTime.of(13, 0);

    // Standard NYSE/NASDAQ full-day closures, 2024-2027: New Year's Day, MLK Day,
    // Washington's Birthday, Good Friday, Memorial Day, Juneteenth, Independence Day,
    // Labor Day, Thanksgiving, Christmas (weekend-observed shifts applied per NYSE
    // convention where relevant).
    private static final Set<LocalDate> HOLIDAYS = Set.of(
            // 2024
            LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 19),
            LocalDate.of(2024, 3, 29), LocalDate.of(2024, 5, 27), LocalDate.of(2024, 6, 19),
            LocalDate.of(2024, 7, 4), LocalDate.of(2024, 9, 2), LocalDate.of(2024, 11, 28),
            LocalDate.of(2024, 12, 25),
            // 2025
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 20), LocalDate.of(2025, 2, 17),
            LocalDate.of(2025, 4, 18), LocalDate.of(2025, 5, 26), LocalDate.of(2025, 6, 19),
            LocalDate.of(2025, 7, 4), LocalDate.of(2025, 9, 1), LocalDate.of(2025, 11, 27),
            LocalDate.of(2025, 12, 25),
            // 2026
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 19), LocalDate.of(2026, 2, 16),
            LocalDate.of(2026, 4, 3), LocalDate.of(2026, 5, 25), LocalDate.of(2026, 6, 19),
            LocalDate.of(2026, 7, 3) /* Jul 4 falls Sat, observed Fri */, LocalDate.of(2026, 9, 7),
            LocalDate.of(2026, 11, 26), LocalDate.of(2026, 12, 25),
            // 2027
            LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 18), LocalDate.of(2027, 2, 15),
            LocalDate.of(2027, 3, 26), LocalDate.of(2027, 5, 31), LocalDate.of(2027, 6, 18) /* Jun 19 falls Sat, observed Fri */,
            LocalDate.of(2027, 7, 5) /* Jul 4 falls Sun, observed Mon */, LocalDate.of(2027, 9, 6),
            LocalDate.of(2027, 11, 25), LocalDate.of(2027, 12, 24) /* Dec 25 falls Sat, observed Fri */
    );

    // Known 1:00pm ET early-close days: day after Thanksgiving every year, plus the day
    // before Independence Day when July 4th itself is a full trading-day holiday (i.e.
    // falls on a weekday and isn't itself weekend-shifted).
    private static final Set<LocalDate> EARLY_CLOSE_DAYS = Set.of(
            LocalDate.of(2024, 7, 3), LocalDate.of(2024, 11, 29),
            LocalDate.of(2025, 7, 3), LocalDate.of(2025, 11, 28),
            LocalDate.of(2026, 11, 27),
            LocalDate.of(2027, 11, 26)
    );

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
        LocalDate date = now.toLocalDate();
        if (HOLIDAYS.contains(date)) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        LocalTime close = EARLY_CLOSE_DAYS.contains(date) ? EARLY_CLOSE : MARKET_CLOSE;
        return !time.isBefore(MARKET_OPEN) && time.isBefore(close);
    }
}
