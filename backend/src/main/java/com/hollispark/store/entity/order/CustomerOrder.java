package com.hollispark.store.entity.order;

import com.hollispark.store.entity.base.BaseEntity;
import com.hollispark.store.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CustomerOrder extends BaseEntity {
    /** What the customer sees and quotes, e.g. HP-24817. The UUID id is internal. */
    @Column(name = "order_number", unique = true)
    private String orderNumber;
    private String email;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    private String address;
    private String city;
    private String pin;
    @Column(name = "ship_method")
    private String shipMethod;
    @Column(name = "promo_code")
    private String promoCode;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal total;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    /** NONE (payments off), PENDING, PAID or FAILED. */
    @Column(name = "payment_status")
    @Builder.Default
    private String paymentStatus = "NONE";
    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;
    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;
    @Column(name = "paid_at")
    private Instant paidAt;
    @Column(name = "admin_note")
    private String adminNote;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<OrderLine> items;
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<StatusEvent> history = new ArrayList<>();

    /** Adds one entry to the order's history (the JSON column is replaced, so Hibernate sees the change). */
    public void record(OrderStatus next, Instant at, String by, String note) {
        List<StatusEvent> events = history == null ? new ArrayList<>() : new ArrayList<>(history);
        events.add(new StatusEvent(next.name(), at, by, note));
        history = events;
    }
}
