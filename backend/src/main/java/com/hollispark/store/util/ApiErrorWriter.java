package com.hollispark.store.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes the standard {@link ApiResponse} failure body from places that run before Spring MVC
 * (security entry points, servlet filters), so every error has the same shape.
 */
@Component
public class ApiErrorWriter {
    private final ObjectMapper mapper;

    public ApiErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse res, int status, String message) throws IOException {
        ApiResponse<Void> body = ApiResponse.failure(message, status);
        body.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getOutputStream(), body);
    }
}
