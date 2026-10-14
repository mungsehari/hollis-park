package com.hollispark.store.enums;

import java.util.Set;

public enum OrderStatus {
    /** Created, waiting for the Razorpay payment. Staff can only cancel it. */
    PENDING_PAYMENT, RECEIVED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;

    /** The order lifecycle: staff can only move an order forward, or cancel it before it ships. */
    public Set<OrderStatus> next() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(CANCELLED);
            case RECEIVED -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(SHIPPED, CANCELLED);
            case SHIPPED -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }
}
