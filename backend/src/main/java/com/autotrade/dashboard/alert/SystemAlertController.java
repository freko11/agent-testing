package com.autotrade.dashboard.alert;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ops-facing system-alert list — mirrors {@code notification.NotificationController}'s shape.
 * No read/unread or acknowledge endpoints since {@link SystemAlert} carries no mutable state
 * (append-only, per this table's design).
 */
@RestController
@RequestMapping("/api/system-alerts")
public class SystemAlertController {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 500;

    private final SystemAlertService systemAlertService;

    public SystemAlertController(SystemAlertService systemAlertService) {
        this.systemAlertService = systemAlertService;
    }

    @GetMapping
    public List<SystemAlertResponse> list(@RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidSystemAlertRequestException("limit must be between 1 and " + MAX_LIMIT + ", got " + limit);
        }
        return systemAlertService.list(limit).stream().map(SystemAlertResponse::from).toList();
    }
}
