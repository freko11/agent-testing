package com.autotrade.dashboard.marketdata;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure unit test — no Spring context, every instant supplied via a fixed Clock. */
class MarketHoursServiceTest {

    private static final ZoneId ET = ZoneId.of("America/New_York");
    // Forced to a specific weekday via TemporalAdjusters so correctness never depends on
    // which day of the week an anchor date happens to fall on.
    private static final LocalDate SUMMER_WEDNESDAY =
            LocalDate.of(2026, 7, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
    private static final LocalDate WINTER_WEDNESDAY =
            LocalDate.of(2026, 1, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

    private MarketHoursService serviceAt(LocalDate date, LocalTime time) {
        ZonedDateTime zoned = ZonedDateTime.of(date, time, ET);
        Clock clock = Clock.fixed(zoned.toInstant(), ET);
        return new MarketHoursService(clock);
    }

    @Test
    void weekdayMidMorning_isOpen() {
        assertTrue(serviceAt(SUMMER_WEDNESDAY, LocalTime.of(10, 0)).isRegularMarketOpen());
    }

    @Test
    void weekdayAtOpenBoundary_inclusive_isOpen() {
        assertTrue(serviceAt(SUMMER_WEDNESDAY, LocalTime.of(9, 30, 0)).isRegularMarketOpen());
    }

    @Test
    void weekdayOneSecondBeforeOpen_isClosed() {
        assertFalse(serviceAt(SUMMER_WEDNESDAY, LocalTime.of(9, 29, 59)).isRegularMarketOpen());
    }

    @Test
    void weekdayAtCloseBoundary_inclusive_isClosed() {
        assertFalse(serviceAt(SUMMER_WEDNESDAY, LocalTime.of(16, 0, 0)).isRegularMarketOpen());
    }

    @Test
    void weekdayOneSecondBeforeClose_isOpen() {
        assertTrue(serviceAt(SUMMER_WEDNESDAY, LocalTime.of(15, 59, 59)).isRegularMarketOpen());
    }

    @Test
    void saturday_isClosed() {
        LocalDate saturday = SUMMER_WEDNESDAY.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
        assertFalse(serviceAt(saturday, LocalTime.of(12, 0)).isRegularMarketOpen());
    }

    @Test
    void sunday_isClosed() {
        LocalDate sunday = SUMMER_WEDNESDAY.with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        assertFalse(serviceAt(sunday, LocalTime.of(12, 0)).isRegularMarketOpen());
    }

    @Test
    void duringEdt_summer_resolvesCorrectlyViaZoneRules() {
        assertTrue(serviceAt(SUMMER_WEDNESDAY, LocalTime.of(10, 0)).isRegularMarketOpen());
    }

    @Test
    void duringEst_winter_resolvesCorrectlyViaZoneRules() {
        assertTrue(serviceAt(WINTER_WEDNESDAY, LocalTime.of(10, 0)).isRegularMarketOpen());
    }

    @Test
    void fixedFederalHoliday_isClosedAllDay() {
        // 2025-12-25, Christmas Day, a Thursday — would otherwise be a normal trading day.
        LocalDate christmas2025 = LocalDate.of(2025, 12, 25);
        assertFalse(serviceAt(christmas2025, LocalTime.of(10, 0)).isRegularMarketOpen());
    }

    @Test
    void dayBeforeHoliday_isUnaffected() {
        // 2025-12-24 is a normal (non-early-close) trading day in this calendar.
        LocalDate dayBeforeChristmas2025 = LocalDate.of(2025, 12, 24);
        assertTrue(serviceAt(dayBeforeChristmas2025, LocalTime.of(10, 0)).isRegularMarketOpen());
    }

    @Test
    void thanksgivingFriday_beforeEarlyCloseCutoff_isOpen() {
        // 2025-11-28, day after Thanksgiving, a known 1:00pm ET early close.
        LocalDate blackFriday2025 = LocalDate.of(2025, 11, 28);
        assertTrue(serviceAt(blackFriday2025, LocalTime.of(12, 59, 59)).isRegularMarketOpen());
    }

    @Test
    void thanksgivingFriday_atEarlyCloseCutoff_isClosed() {
        LocalDate blackFriday2025 = LocalDate.of(2025, 11, 28);
        assertFalse(serviceAt(blackFriday2025, LocalTime.of(13, 0, 0)).isRegularMarketOpen());
    }

    @Test
    void thanksgivingFriday_afterEarlyCloseButBeforeNormalClose_isClosed() {
        // Would be open under the plain 16:00 close — must reflect the 13:00 early close instead.
        LocalDate blackFriday2025 = LocalDate.of(2025, 11, 28);
        assertFalse(serviceAt(blackFriday2025, LocalTime.of(15, 0)).isRegularMarketOpen());
    }
}
