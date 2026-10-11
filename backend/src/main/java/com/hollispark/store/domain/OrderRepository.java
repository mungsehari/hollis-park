package com.hollispark.store.domain;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID>, JpaSpecificationExecutor<CustomerOrder> {
    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByRazorpayOrderId(String razorpayOrderId);

    /** Payment confirmation (browser) and webhook (Razorpay) can arrive together; the row lock makes them take turns. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.orderNumber = :orderNumber")
    Optional<CustomerOrder> lockByOrderNumber(@Param("orderNumber") String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.razorpayOrderId = :razorpayOrderId")
    Optional<CustomerOrder> lockByRazorpayOrderId(@Param("razorpayOrderId") String razorpayOrderId);

    List<CustomerOrder> findByCreatedAtAfterOrderByCreatedAtDesc(Instant after);

    List<CustomerOrder> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    /** Next customer-facing order number (HP-24818 style). The sequence never repeats and never goes back. */
    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    long nextOrderNumber();

    /** One query for all the dashboard's status counters. Rows are {status, count}. */
    @Query("select o.status, count(o) from CustomerOrder o group by o.status")
    List<Object[]> countByStatusGrouped();
}
