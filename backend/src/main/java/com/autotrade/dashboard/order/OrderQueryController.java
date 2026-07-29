package com.autotrade.dashboard.order;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/{id}/refresh")
    public OrderResponse refreshOrder(@PathVariable Long id) {
        return orderService.refreshOrder(id);
    }
}
