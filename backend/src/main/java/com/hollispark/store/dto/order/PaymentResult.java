package com.hollispark.store.dto.order;

public record PaymentResult(String orderNumber, String status, String paymentStatus) {}
