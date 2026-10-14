package com.hollispark.store.controller.payment;

import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Razorpay → store. Add this URL in the Razorpay dashboard (Webhooks) with the same secret as RAZORPAY_WEBHOOK_SECRET. */
@Tag(name = "Payments", description = "Razorpay webhook")
@RestController
@RequestMapping("/api/v1/payments/razorpay")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final PaymentService payments;

    @Operation(summary = "Razorpay payment events (signature checked with the webhook secret)")
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(@RequestBody String rawBody,
                                                     @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        payments.handleWebhook(rawBody, signature);
        return ResponseEntity.ok(ApiResponse.success(null, "ok"));
    }
}
