package com.hackathon.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

/**
 * Standardized API response envelope for all endpoints.
 *
 * <p>Adheres to a consistent contract so API consumers can always expect
 * {@code success}, {@code data} (on success), {@code message}, {@code timestamp},
 * and a {@code traceId} for support correlation.
 *
 * @param <T> the payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp,
        String traceId
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, Instant.now(), UUID.randomUUID().toString());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now(), UUID.randomUUID().toString());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, Instant.now(), UUID.randomUUID().toString());
    }
}
