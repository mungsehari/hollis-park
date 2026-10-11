package com.hollispark.store.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByRazorpayOrderId(String razorpayOrderId);

    List<CustomerOrder> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    List<CustomerOrder> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);
}
