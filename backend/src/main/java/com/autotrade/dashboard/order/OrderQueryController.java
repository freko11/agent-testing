package com.autotrade.dashboard.order;

import com.autotrade.dashboard.common.PagedResponse;
import com.autotrade.dashboard.common.TradingMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Order status/history (E5-F3-S1) — a cross-ticker resource, mirroring
 * {@code /api/watchlist}'s precedent, kept separate from {@link
 * OrderController} (which is submission-only, scoped under
 * {@code /api/tickers/{symbol}/orders}).
 */
@RestController
@RequestMapping("/api/orders")
public class OrderQueryController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;
    private static final int DEFAULT_AUDIT_PAGE_SIZE = 25;
    private static final int MAX_AUDIT_PAGE_SIZE = 100;

    private final OrderService orderService;

    public OrderQueryController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidTradeRequestException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        return orderService.listOrders(limit);
    }

    /** Audit-trail viewer (E6-F3-S3) — a distinct, newest-first paginated resource, not the limit-only {@link #listOrders} shape. */
    @GetMapping("/audit-entries")
    public PagedResponse<AuditEntryResponse> listAuditEntries(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "" + DEFAULT_AUDIT_PAGE_SIZE) int size) {
        if (page < 0) {
            throw new InvalidTradeRequestException("page must be >= 0, got " + page);
        }
        if (size < 1 || size > MAX_AUDIT_PAGE_SIZE) {
            throw new InvalidTradeRequestException("size must be between 1 and " + MAX_AUDIT_PAGE_SIZE + ", got " + size);
        }
        return orderService.listAuditEntries(page, size);
    }

    @PostMapping("/{id}/refresh")
    public OrderResponse refreshOrder(@PathVariable Long id) {
        return orderService.refreshOrder(id);
    }

    /** CSV export for a date range (E5-F3-S2) — see {@link OrderService#exportOrdersCsv} for the UTC-day/mode-default semantics. */
    @GetMapping("/export")
    public ResponseEntity<String> exportOrders(@RequestParam LocalDate start, @RequestParam LocalDate end,
                                                @RequestParam(required = false) TradingMode mode) {
        String csv = orderService.exportOrdersCsv(start, end, mode);
        String filename = "trade-history-" + start + "-to-" + end + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
