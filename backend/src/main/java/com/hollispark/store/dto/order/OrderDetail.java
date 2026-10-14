package com.hollispark.store.dto.order;

import com.hollispark.store.entity.order.CustomerOrder;
import com.hollispark.store.entity.order.OrderLine;
import com.hollispark.store.entity.order.StatusEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
