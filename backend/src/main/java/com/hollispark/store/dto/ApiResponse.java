package com.hollispark.store.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The one envelope every JSON endpoint answers with, success or failure:
 * {@code {success, status, message, data[, requestId]}}. {@code requestId} is only set on failures, so a
 * customer can quote it and staff can find the matching log lines.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private int status;
    private String message;
    private T data;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "OK", data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, 200, message, data, null);
    }

    public static <T> ApiResponse<T> success(T data, int status, String message) {
        return new ApiResponse<>(true, status, message, data, null);
    }

    /**
     * The only failure factory. Argument order is always (message, status), matching
     * {@code success(data, status, message)}. Do not add a (status, message) overload: the two would be
     * indistinguishable to the compiler and a transposed call would compile.
     */
    public static <T> ApiResponse<T> failure(String message, int status) {
        return new ApiResponse<>(false, status, message, null, null);
    }

    /** Failure with structured detail, e.g. the per-field validation errors. */
    public static <T> ApiResponse<T> failure(String message, int status, T data) {
        return new ApiResponse<>(false, status, message, data, null);
    }
}
