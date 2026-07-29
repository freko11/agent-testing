package com.autotrade.dashboard.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * App-wide {@link Clock} bean so tests can supply {@code Clock.fixed(...)}
 * instead of depending on wall-clock time. Originally lived in {@code
 * marketdata.MarketDataClientConfig} (its first consumer, {@code
 * MarketHoursService}) with a note to move it here once a second, unrelated
 * consumer showed up — {@code brokeradapter.AlpacaTradingAdapter} (E4-F2-S1)
 * is that second consumer.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
