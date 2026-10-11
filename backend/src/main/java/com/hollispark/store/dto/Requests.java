package com.hollispark.store.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public final class Requests {
    private Requests() {}

    public record PromoRequest(@NotBlank @Size(max = 40) String code) {}

    public record PromoResponse(String code, BigDecimal rate) {}

    public record PaymentConfirmation(
            @NotBlank @Size(max = 80) String razorpayOrderId,
            @NotBlank @Size(max = 80) String razorpayPaymentId,
            @NotBlank @Size(max = 200) String razorpaySignature) {}

    public record PaymentResult(String orderNumber, String status, String paymentStatus) {}

    public record ContactRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 200) String email,
            @NotBlank @Size(max = 40) String topic,
            @Size(max = 40) String order,
            @Size(max = 80) String slot,
            @NotBlank @Size(min = 10, max = 4000) String message) {}

    public record ContactResponse(String status) {}

    public record NewsletterRequest(
            @NotBlank @Email @Size(max = 200) String email,
            @Size(max = 10) List<@Size(max = 30) String> topics) {}

    public record NewsletterResponse(String status, String promoCode, BigDecimal promoRate) {}
}
