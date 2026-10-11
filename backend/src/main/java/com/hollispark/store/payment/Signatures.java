package com.hollispark.store.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** HMAC-SHA256 checks Razorpay documents for payments and webhooks, compared in constant time. */
public final class Signatures {
    private Signatures() {}

    public static String hmacHex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] d = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String expectedHex, String actual) {
        if (expectedHex == null || actual == null) return false;
        return MessageDigest.isEqual(expectedHex.getBytes(StandardCharsets.UTF_8), actual.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /** Checkout callback: HMAC(razorpay_order_id + "|" + razorpay_payment_id, key_secret). */
    public static boolean validPayment(String keySecret, String orderId, String paymentId, String signature) {
        return matches(hmacHex(keySecret, orderId + "|" + paymentId), signature);
    }

    /** Webhook: HMAC(raw request body, webhook_secret). */
    public static boolean validWebhook(String webhookSecret, String rawBody, String signature) {
        return webhookSecret != null && !webhookSecret.isBlank() && matches(hmacHex(webhookSecret, rawBody), signature);
    }
}
