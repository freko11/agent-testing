package com.autotrade.dashboard.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Submits a bracket order for a ticker's current BUY/SELL signal (E5-F2-S1) — "click Trade" hits this directly, no confirmation step yet (that's E5-F2-S2). */
@RestController
@RequestMapping("/api/tickers")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{symbol}/orders")
    public ResponseEntity<TradeOrderResponse> placeOrder(@PathVariable String symbol,
                                                           @Valid @RequestBody PlaceOrderRequest request) {
        TradeOrderResponse response = orderService.submitOrder(symbol, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
