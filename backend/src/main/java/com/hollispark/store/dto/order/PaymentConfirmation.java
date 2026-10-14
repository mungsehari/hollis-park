package com.hollispark.store.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentConfirmation(
        @NotBlank @Size(max = 80) String razorpayOrderId,
        @NotBlank @Size(max = 80) String razorpayPaymentId,
        @NotBlank @Size(max = 200) String razorpaySignature) {}
