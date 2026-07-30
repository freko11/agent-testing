package com.autotrade.dashboard.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} beans app-wide — needed for the first
 * background job in this codebase, {@code notification.WatchlistSignalPoller}
 * (E5-F4-S1). A dedicated config class (not just an annotation on {@code
 * BackendApplication}) so it's easy to find alongside {@link ClockConfig}.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
