package com.hollispark.store.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Brand and market format, bound from {@code store.*}. Tax, delivery and promo values here are
 * only first-start defaults; the live values are in the database (see SettingsService).
 */
@ConfigurationProperties(prefix = "store")
public record StoreProperties(
        String name,
        String storeName,
        String orderPrefix,
        String locale,
        String currency,
        String sizeLabel,
        Postcode postcode,
        Defaults defaults,
        BootstrapAdmin bootstrapAdmin,
        List<String> corsAllowedOrigins,
        Media media,
        Razorpay razorpay,
        RateLimit rateLimit) {

    /** Requests allowed per client address in ten minutes on the order form; the other forms scale from it. */
    public record RateLimit(boolean enabled, int perTenMinutes) {}

    public record Postcode(String label, String pattern, String hint, boolean numeric) {}

    public record Defaults(String taxMode, BigDecimal taxRate, String taxName, int freeShipFrom, int shipFee,
                           int expressFee, Map<String, BigDecimal> promoCodes) {}

    public record BootstrapAdmin(String email, String password, String name) {}

    public record Razorpay(String keyId, String keySecret, String webhookSecret) {
        public boolean enabled() {
            return keyId != null && !keyId.isBlank() && keySecret != null && !keySecret.isBlank();
        }

        public boolean testMode() {
            return keyId != null && keyId.startsWith("rzp_test_");
        }
    }

    /** storage: "local" (folder served at /media) or "s3" (bucket served by CloudFront at /media). */
    public record Media(String storage, String localDir, String bucket, String region, String publicBaseUrl) {}
}
