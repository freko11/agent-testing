package com.autotrade.dashboard.watchlist;

import com.autotrade.dashboard.marketdata.TickerSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Saved tickers a user wants to revisit without retyping (E3-F3-S1). */
@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    public record AddWatchlistEntryRequest(@NotBlank @Size(max = 20) String symbol) {
    }

    public record WatchlistEntryResponse(Long id, TickerSummary ticker, Instant addedAt) {
        static WatchlistEntryResponse from(WatchlistEntry entry) {
            return new WatchlistEntryResponse(entry.getId(), TickerSummary.from(entry.getTicker()), entry.getCreatedAt());
        }
    }

    @GetMapping
    public List<WatchlistEntryResponse> list() {
        return watchlistService.list().stream().map(WatchlistEntryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<WatchlistEntryResponse> add(@Valid @RequestBody AddWatchlistEntryRequest request) {
        boolean existedBefore = watchlistService.contains(request.symbol());
        WatchlistEntry entry = watchlistService.add(request.symbol());
        HttpStatus status = existedBefore ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(WatchlistEntryResponse.from(entry));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> remove(@PathVariable String symbol) {
        watchlistService.remove(symbol);
        return ResponseEntity.noContent().build();
    }
}
