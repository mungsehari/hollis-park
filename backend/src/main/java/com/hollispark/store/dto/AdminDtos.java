package com.hollispark.store.dto;

import com.hollispark.store.domain.ContactMessage;
import com.hollispark.store.domain.CustomerOrder;
import com.hollispark.store.domain.OrderLine;
import com.hollispark.store.domain.PromoCode;
import com.hollispark.store.domain.StaffUser;
import com.hollispark.store.domain.StatusEvent;
import com.hollispark.store.domain.Subscriber;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public final class AdminDtos {
    private AdminDtos() {}

    public record PageDto<T>(List<T> items, long total, int page, int size) {
        public static <T> PageDto<T> of(Page<T> p) {
            return new PageDto<>(p.getContent(), p.getTotalElements(), p.getNumber(), p.getSize());
        }
    }

    /* ---------- auth ---------- */
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record PasswordChange(@NotBlank String current, @NotBlank @Size(min = 10, max = 200) String next) {}

    public record Me(UUID id, String email, String name, String role) {
        public static Me of(StaffUser u) {
            return new Me(u.getId(), u.getEmail(), u.getName(), u.getRole().name());
        }
    }

    /* ---------- orders ---------- */
    public record OrderSummary(UUID id, String orderNumber, String name, String email, String city, int items,
                               BigDecimal total, String status, String paymentStatus, String ship, Instant createdAt) {
        public static OrderSummary of(CustomerOrder o) {
            int n = o.getItems() == null ? 0 : o.getItems().stream().mapToInt(OrderLine::qty).sum();
            return new OrderSummary(o.getId(), o.getOrderNumber(), o.getFirstName() + " " + o.getLastName(), o.getEmail(),
                    o.getCity(), n, o.getTotal(), o.getStatus().name(), o.getPaymentStatus(), o.getShipMethod(),
                    o.getCreatedAt());
        }
    }

    public record OrderDetail(UUID id, String orderNumber, String status, List<String> nextStatuses, String email,
                              String first, String last, String address, String city, String pin, String ship,
                              String promo, BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                              BigDecimal tax, BigDecimal total, String adminNote, List<OrderLine> items,
                              List<StatusEvent> history, Instant createdAt, Instant updatedAt,
                              String paymentStatus, String razorpayPaymentId, Instant paidAt) {
        public static OrderDetail of(CustomerOrder o) {
            return new OrderDetail(o.getId(), o.getOrderNumber(), o.getStatus().name(),
                    o.getStatus().next().stream().map(Enum::name).sorted().toList(), o.getEmail(), o.getFirstName(),
                    o.getLastName(), o.getAddress(), o.getCity(), o.getPin(), o.getShipMethod(), o.getPromoCode(),
                    o.getSubtotal(), o.getDiscount(), o.getShipping(), o.getTax(), o.getTotal(), o.getAdminNote(),
                    o.getItems(), o.getHistory(), o.getCreatedAt(), o.getUpdatedAt(), o.getPaymentStatus(),
                    o.getRazorpayPaymentId(), o.getPaidAt());
        }
    }

    public record OrderUpdate(String status, @Size(max = 500) String note, @Size(max = 2000) String adminNote) {}

    /* ---------- products ---------- */
    public record ProductAdmin(ProductDto product, boolean active, Instant updatedAt) {}

    public record ActiveUpdate(@NotNull Boolean active) {}

    /* ---------- messages and subscribers ---------- */
    public record MessageDto(UUID id, String name, String email, String topic, String orderNumber, String slot,
                             String message, String status, Instant createdAt) {
        public static MessageDto of(ContactMessage m) {
            return new MessageDto(m.getId(), m.getName(), m.getEmail(), m.getTopic(), m.getOrderNumber(), m.getSlot(),
                    m.getMessage(), m.getStatus(), m.getCreatedAt());
        }
    }

    public record MessageUpdate(@NotBlank @Pattern(regexp = "NEW|HANDLED") String status) {}

    public record SubscriberDto(UUID id, String email, List<String> topics, Instant createdAt) {
        public static SubscriberDto of(Subscriber s) {
            return new SubscriberDto(s.getId(), s.getEmail(), s.getTopics(), s.getCreatedAt());
        }
    }

    /* ---------- staff ---------- */
    public record StaffDto(UUID id, String email, String name, String role, boolean active, Instant createdAt, Instant lastLoginAt) {
        public static StaffDto of(StaffUser u) {
            return new StaffDto(u.getId(), u.getEmail(), u.getName(), u.getRole().name(), u.isActive(), u.getCreatedAt(),
                    u.getLastLoginAt());
        }
    }

    public record StaffCreate(@NotBlank @Email @Size(max = 200) String email, @NotBlank @Size(max = 120) String name,
                              @NotBlank @Pattern(regexp = "ADMIN|STAFF") String role,
                              @NotBlank @Size(min = 10, max = 200) String password) {}

    public record StaffUpdate(@Size(max = 120) String name, @Pattern(regexp = "ADMIN|STAFF") String role, Boolean active) {}

    public record PasswordReset(@NotBlank @Size(min = 10, max = 200) String password) {}

    /* ---------- settings and promo codes ---------- */
    public record SettingsDto(@NotBlank @Pattern(regexp = "included|added") String taxMode,
                              @NotNull @DecimalMin("0") @DecimalMax("0.9999") BigDecimal taxRate,
                              @NotBlank @Size(max = 40) String taxName,
                              @Min(0) @Max(10_000_000) int freeShipFrom,
                              @Min(0) @Max(1_000_000) int shipFee,
                              @Min(0) @Max(1_000_000) int expressFee) {}

    public record PromoDto(String code, BigDecimal rate, boolean active, Instant createdAt) {
        public static PromoDto of(PromoCode p) {
            return new PromoDto(p.getCode(), p.getRate(), p.isActive(), p.getCreatedAt());
        }
    }

    public record PromoCreate(@NotBlank @Pattern(regexp = "[A-Za-z0-9]{3,40}", message = "Use 3-40 letters or digits.") String code,
                              @NotNull @DecimalMin(value = "0.01") @DecimalMax("0.9") BigDecimal rate) {}

    public record PromoUpdate(Boolean active, @DecimalMin(value = "0.01") @DecimalMax("0.9") BigDecimal rate) {}
}
