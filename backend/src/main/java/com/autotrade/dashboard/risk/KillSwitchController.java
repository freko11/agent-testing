package com.autotrade.dashboard.risk;

import com.autotrade.dashboard.order.OrderService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The global kill switch (E6-F2-S2) — a standalone top-level resource mirroring {@code
 * /api/trading-mode}'s shape. Normal session auth only, same as every other mutating
 * endpoint in this app. {@link #engage} flips state to {@code ENGAGED} <em>before</em>
 * running the cancel sweep, so the "block new submissions" guarantee never depends on
 * cancellation succeeding — a partial or total cancel failure still leaves new orders
 * blocked.
 *
 * <p>Reads the current username from {@link SecurityContextHolder} directly rather than
 * an {@code Authentication} method parameter — both work identically against the real
 * security filter chain, but only the former is populated by {@code @WithMockUser} in a
 * {@code @WebMvcTest} slice with the filter chain disabled ({@code addFilters = false},
 * this app's established slice-test convention).
 */
@RestController
@RequestMapping("/api/kill-switch")
public class KillSwitchController {

    private final KillSwitchService killSwitchService;
    private final OrderService orderService;

    public KillSwitchController(KillSwitchService killSwitchService, OrderService orderService) {
        this.killSwitchService = killSwitchService;
        this.orderService = orderService;
    }

    @GetMapping
    public KillSwitchResponse current() {
        return killSwitchService.currentState();
    }

    @PostMapping("/engage")
    public EngageKillSwitchResponse engage() {
        KillSwitchResponse state = killSwitchService.engage(currentUsername());
        KillSwitchCancelSummary summary = orderService.cancelAllOpenOrders();
        return new EngageKillSwitchResponse(state, summary);
    }

    @PostMapping("/clear")
    public KillSwitchResponse clear() {
        return killSwitchService.clear(currentUsername());
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
