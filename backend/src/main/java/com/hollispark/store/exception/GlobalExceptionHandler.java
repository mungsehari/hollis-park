package com.hollispark.store.exception;

import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.filter.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Every failure that reaches Spring MVC leaves through here, as an {@link ApiResponse} envelope.
 * Rules (see docs/error-handling.md):
 * <ul>
 *   <li>A client mistake is never a 500: bad input, missing parameter, wrong verb, unknown route → 4xx.</li>
 *   <li>A 5xx never returns the exception's message; the detail is logged, the client gets a generic sentence.</li>
 *   <li>Client mistakes are logged as one WARN line without a stack trace.</li>
 * </ul>
 * Failures that happen before MVC (security, filters, the /error fallback) use the same envelope via
 * {@code ApiErrorWriter} and {@code ApiErrorController}. Do not add a second {@code @ControllerAdvice}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static <T> ResponseEntity<ApiResponse<T>> reply(HttpStatus status, String message, T data) {
        ApiResponse<T> body = ApiResponse.failure(message, status.value(), data);
        body.setRequestId(MDC.get(RequestIdFilter.MDC_KEY));
        return ResponseEntity.status(status).body(body);
    }

    private static ResponseEntity<ApiResponse<Void>> reply(HttpStatus status, String message) {
        return reply(status, message, null);
    }

    private static void clientError(HttpServletRequest req, HttpStatus status, String message) {
        log.warn("{} {} -> {} {}", req.getMethod(), req.getRequestURI(), status.value(), message);
    }

    /** Our own deliberate failures: the message was written for the user, so it is returned as is. */
    @ExceptionHandler(AppException.class)
    ResponseEntity<ApiResponse<Void>> app(AppException e, HttpServletRequest req) {
        if (e.status().is5xxServerError()) log.error("{} {} failed: {}", req.getMethod(), req.getRequestURI(), e.getMessage(), e.getCause());
        else clientError(req, e.status(), e.getMessage());
        return reply(e.status(), e.getMessage());
    }

    /** {@code @Valid} body failed. The per-field messages go in {@code data} so the form can show them. → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> invalid(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        clientError(req, HttpStatus.BAD_REQUEST, "validation " + fields.keySet());
        return reply(HttpStatus.BAD_REQUEST, "Some details need attention.", fields);
    }

    /** Body is not JSON, or a value has the wrong type (e.g. text where a number belongs). → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> unreadable(HttpMessageNotReadableException e, HttpServletRequest req) {
        clientError(req, HttpStatus.BAD_REQUEST, "unreadable body");
        return reply(HttpStatus.BAD_REQUEST, "The request body could not be read.");
    }

    /** A path variable or parameter that cannot be converted, e.g. "abc" where a UUID is expected. → 400 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest req) {
        String type = e.getRequiredType() == null ? "value" : e.getRequiredType().getSimpleName();
        String message = "Invalid value for '" + e.getName() + "': expected a valid " + type + ".";
        clientError(req, HttpStatus.BAD_REQUEST, message);
        return reply(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> missingParam(MissingServletRequestParameterException e, HttpServletRequest req) {
        String message = "Required parameter '" + e.getParameterName() + "' is missing.";
        clientError(req, HttpStatus.BAD_REQUEST, message);
        return reply(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ResponseEntity<ApiResponse<Void>> missingHeader(MissingRequestHeaderException e, HttpServletRequest req) {
        String message = "Required header '" + e.getHeaderName() + "' is missing.";
        clientError(req, HttpStatus.BAD_REQUEST, message);
        return reply(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiResponse<Void>> missingPart(MissingServletRequestPartException e, HttpServletRequest req) {
        clientError(req, HttpStatus.BAD_REQUEST, "missing part " + e.getRequestPartName());
        return reply(HttpStatus.BAD_REQUEST, "Choose an image to upload.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest req) {
        clientError(req, HttpStatus.PAYLOAD_TOO_LARGE, "upload too large");
        return reply(HttpStatus.PAYLOAD_TOO_LARGE, "Images can be up to 8 MB.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> mediaType(HttpMediaTypeNotSupportedException e, HttpServletRequest req) {
        clientError(req, HttpStatus.UNSUPPORTED_MEDIA_TYPE, String.valueOf(e.getContentType()));
        return reply(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Content-Type for this endpoint.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> method(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
        clientError(req, HttpStatus.METHOD_NOT_ALLOWED, e.getMethod());
        return reply(HttpStatus.METHOD_NOT_ALLOWED, "This method is not supported here.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> noRoute(NoResourceFoundException e, HttpServletRequest req) {
        return reply(HttpStatus.NOT_FOUND, "Not found.");
    }

    /**
     * {@code @PreAuthorize} rejected the caller. Method security throws inside the handler, so Spring
     * Security's own entry point never sees it: without this every denial would become a 500.
     * Not signed in → 401, signed in without the role → 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> denied(AccessDeniedException e, HttpServletRequest req) {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = who == null || who instanceof AnonymousAuthenticationToken || !who.isAuthenticated();
        HttpStatus status = anonymous ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        clientError(req, status, "access denied");
        return reply(status, anonymous ? "Please sign in." : "You do not have permission to do this.");
    }

    /** Two people changed the same row at once (BaseEntity version); the second save is refused. → 409 */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> concurrent(OptimisticLockingFailureException e, HttpServletRequest req) {
        clientError(req, HttpStatus.CONFLICT, "concurrent update");
        return reply(HttpStatus.CONFLICT, "This was just changed by someone else. Reload and try again.");
    }

    /** A database constraint stopped the write (duplicate, foreign key). The constraint name is only logged. → 409 */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Void>> integrity(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("{} {} -> 409 {}", req.getMethod(), req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return reply(HttpStatus.CONFLICT, "That conflicts with existing data.");
    }

    /** Anything else is our bug. Full detail to the log, a generic sentence to the client. → 500 */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception e, HttpServletRequest req) {
        log.error("{} {} failed", req.getMethod(), req.getRequestURI(), e);
        return reply(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");
    }
}
