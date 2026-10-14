package com.hollispark.store.dto.order;

import java.math.BigDecimal;

public record OrderResponse(String orderNumber, String status, String paymentStatus, BigDecimal subtotal,
                            BigDecimal discount, BigDecimal shipping, BigDecimal tax, BigDecimal total, String ship,
                            Payment payment) {

    /** Present only when online payment is on: what the browser needs to open Razorpay Checkout. */
    public record Payment(String keyId, String razorpayOrderId, long amountPaise, String currency, boolean testMode) {}
}
