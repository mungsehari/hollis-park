package com.hollispark.store.controller;

import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.filter.RequestIdFilter;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The /error fallback: errors raised outside Spring MVC (a filter's sendError, an exception thrown while
 * writing a response) end up here. Replaces Spring Boot's default error page so even these answer with
 * the {@link ApiResponse} envelope and never expose exception text.
 */
@Hidden
@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("${server.error.path:/error}")
    public ResponseEntity<ApiResponse<Void>> error(HttpServletRequest request) {
        Object code = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        HttpStatus status = code instanceof Integer i ? HttpStatus.resolve(i) : null;
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = switch (status) {
            case NOT_FOUND -> "Not found.";
            case UNAUTHORIZED -> "Please sign in.";
            case FORBIDDEN -> "You do not have permission to do this.";
            case METHOD_NOT_ALLOWED -> "This method is not supported here.";
            case PAYLOAD_TOO_LARGE -> "The request is too large.";
            default -> status.is4xxClientError() ? "The request could not be processed." : "Something went wrong. Please try again.";
        };
        ApiResponse<Void> body = ApiResponse.failure(message, status.value());
        body.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        return ResponseEntity.status(status).body(body);
    }
}
