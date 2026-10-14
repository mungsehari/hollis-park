package com.hollispark.store.dto.order;

import com.hollispark.store.domain.CustomerOrder;
import com.hollispark.store.domain.OrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummary(UUID id, String orderNumber, String name, String email, String city, int items,
                           BigDecimal total, String status, String paymentStatus, String ship, Instant createdAt) {
    public static OrderSummary of(CustomerOrder o) {
        int n = o.getItems() == null ? 0 : o.getItems().stream().mapToInt(OrderLine::qty).sum();
        return new OrderSummary(o.getId(), o.getOrderNumber(), o.getFirstName() + " " + o.getLastName(), o.getEmail(),
                o.getCity(), n, o.getTotal(), o.getStatus().name(), o.getPaymentStatus(), o.getShipMethod(),
                o.getCreatedAt());
    }
}
