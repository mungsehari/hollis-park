package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class CustomerOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "order_number", unique = true)
    public String orderNumber;
    public String email;
    @Column(name = "first_name")
    public String first;
    @Column(name = "last_name")
    public String last;
    public String address;
    public String city;
    public String pin;
    @Column(name = "ship_method")
    public String ship;
    @Column(name = "promo_code")
    public String promo;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal shipping;
    public BigDecimal tax;
    public BigDecimal total;
    /** Payment is not integrated: new orders are RECEIVED and never charged. */
    @Enumerated(EnumType.STRING)
    public OrderStatus status;
    /** NONE (payments off), PENDING, PAID or FAILED. */
    @Column(name = "payment_status")
    public String paymentStatus = "NONE";
    @Column(name = "razorpay_order_id")
    public String razorpayOrderId;
    @Column(name = "razorpay_payment_id")
    public String razorpayPaymentId;
    @Column(name = "paid_at")
    public Instant paidAt;
    @Column(name = "admin_note")
    public String adminNote;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<OrderLine> items;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<StatusEvent> history = new ArrayList<>();
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
