package com.autotrade.dashboard.notification;

import com.autotrade.dashboard.signal.SignalCall;
import com.autotrade.dashboard.signal.SignalCallEntry;
import com.autotrade.dashboard.signal.SignalCallEntryRepository;
import com.autotrade.dashboard.signal.SignalService;
import com.autotrade.dashboard.ticker.Ticker;
import com.autotrade.dashboard.watchlist.WatchlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Watches every watchlisted ticker for a Buy/Sell/Hold call change (the
 * watchlist half of E5-F4-S1's AC) — the first background/scheduled job in
 * this codebase, a deliberate departure from the "manual refresh only" bias
 * every prior story established (E5-F3-S1's order-status polling, E4-F3-S2's
 * no-auto-flatten decision) because there's no user action to hook a signal
 * change onto in the first place.
 *
 * <p>Polls sequentially, not in parallel — this is a single-user tool with an
 * expected-small watchlist, so a small fixed pause between tickers keeps
 * every cycle comfortably under Alpaca's/Binance's rate limits without an
 * adaptive throttling framework. Uses {@code fixedDelay} (not {@code
 * fixedRate}) so a slow cycle never overlaps the next one. Every per-ticker
 * failure (market closed, rate-limited, insufficient history, etc. — all
 * {@code RuntimeException}s in this codebase) is caught, logged, and skipped:
 * one ticker's failure must never abort the batch, and a stock ticker outside
 * market hours will routinely throw on every off-hours cycle, which is
 * expected, not an error.
 *
 * <p>Gated by {@code notification.watchlist-poll.enabled} (default {@code
 * true}, forced {@code false} in the test profile) so {@code @SpringBootTest}
 * classes never trigger a real network call via this bean — this codebase's
 * repeatedly-stated "no live network calls in CI" discipline.
 */
@Component
@ConditionalOnProperty(name = "notification.watchlist-poll.enabled", havingValue = "true", matchIfMissing = true)
public class WatchlistSignalPoller {

    private static final Logger log = LoggerFactory.getLogger(WatchlistSignalPoller.class);
    private static final int SIGNAL_LIMIT = 200;
    private static final long INTER_TICKER_PAUSE_MS = 750;

    private final WatchlistService watchlistService;
    private final SignalService signalService;
    private final SignalCallEntryRepository signalCallEntryRepository;
    private final NotificationService notificationService;

    public WatchlistSignalPoller(WatchlistService watchlistService, SignalService signalService,
                                  SignalCallEntryRepository signalCallEntryRepository,
                                  NotificationService notificationService) {
        this.watchlistService = watchlistService;
        this.signalService = signalService;
        this.signalCallEntryRepository = signalCallEntryRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${notification.watchlist-poll.fixed-delay-ms}")
    public void pollWatchlist() {
        List<Ticker> tickers = watchlistService.listTickers();
        for (int i = 0; i < tickers.size(); i++) {
            pollOne(tickers.get(i));
            if (i < tickers.size() - 1) {
                sleep(INTER_TICKER_PAUSE_MS);
            }
        }
    }

    private void pollOne(Ticker ticker) {
        try {
            Optional<SignalCallEntry> previous = signalCallEntryRepository.findTopByTickerOrderByCreatedAtDescIdDesc(ticker);
            SignalService.SignalComputation computation = signalService.computeSignalWithProvenance(ticker.getSymbol(), SIGNAL_LIMIT);

            if (previous.isEmpty()) {
                // First-ever poll for this ticker — establish the baseline silently, don't notify.
                return;
            }
            SignalCall previousCall = previous.get().getCall();
            SignalCall currentCall = computation.response().call();
            if (previousCall != currentCall) {
                SignalCallEntry currentEntry = signalCallEntryRepository
                        .findTopByTickerOrderByCreatedAtDescIdDesc(ticker)
                        .orElseThrow(() -> new IllegalStateException(
                                "Signal computation for " + ticker.getSymbol() + " did not persist a SignalCallEntry"));
                notificationService.recordSignalChange(ticker, previousCall, currentEntry);
            }
        } catch (RuntimeException e) {
            log.debug("Skipping watchlist signal poll for {}: {}", ticker.getSymbol(), e.getMessage());
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
