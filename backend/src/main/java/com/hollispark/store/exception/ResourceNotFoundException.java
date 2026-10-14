package com.hollispark.store.exception;

import org.springframework.http.HttpStatus;

/** The route is valid but the id, slug or order number resolves to nothing. Only for lookups → NOT_FOUND. */
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
