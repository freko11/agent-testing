package com.autotrade.dashboard.notification;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * In-app notification list (E5-F4-S1) — mirrors {@code /api/watchlist}/
 * {@code /api/orders}'s precedent as a standalone top-level resource.
 * Marking read is idempotent-by-convention (a no-op on an unknown/
 * already-read id, matching {@code WatchlistService.remove}'s DELETE-style
 * precedent), so no dedicated not-found error code was needed.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list(@RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidNotificationRequestException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        return notificationService.list(limit).stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount() {
        return Map.of("unreadCount", notificationService.countUnread());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead();
        return ResponseEntity.noContent().build();
    }
}
