package com.hollispark.store.web;

import com.hollispark.store.payment.PaymentService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Razorpay -> store. Add this URL in the Razorpay dashboard (Webhooks) with the same secret as RAZORPAY_WEBHOOK_SECRET. */
@RestController
@RequestMapping("/api/payments/razorpay")
public class PaymentWebhookController {
    private final PaymentService payments;

    public PaymentWebhookController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/webhook")
    public Map<String, String> webhook(@RequestBody String rawBody,
                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        payments.handleWebhook(rawBody, signature);
        return Map.of("status", "ok");
    }
}
