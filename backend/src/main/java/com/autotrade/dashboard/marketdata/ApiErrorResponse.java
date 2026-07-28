package com.autotrade.dashboard.marketdata;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(String error, String message, String source) {

    static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(error, message, null);
    }

    static ApiErrorResponse of(String error, String message, String source) {
        return new ApiErrorResponse(error, message, source);
    }
}
