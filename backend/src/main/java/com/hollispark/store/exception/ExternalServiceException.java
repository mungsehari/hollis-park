package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/**
 * A partner service (Razorpay, S3) failed or could not be reached → 502. The message is the safe,
 * customer-facing sentence; the cause carries the technical detail and is only logged.
 */
public class ExternalServiceException extends AppException {
    public ExternalServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
