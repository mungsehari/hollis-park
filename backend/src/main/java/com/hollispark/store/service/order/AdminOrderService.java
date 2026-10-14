package com.hollispark.store.service.order;

import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.order.OrderDetail;
import com.hollispark.store.dto.order.OrderSummary;
import com.hollispark.store.dto.order.OrderUpdate;
import com.hollispark.store.entity.order.CustomerOrder;
import com.hollispark.store.enums.OrderStatus;
import com.hollispark.store.exception.BadRequestException;
import com.hollispark.store.exception.ConflictException;
import com.hollispark.store.exception.ResourceNotFoundException;
import com.hollispark.store.repository.order.OrderRepository;
import com.hollispark.store.util.PageRequests;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orders as staff see them: search, detail, and moving an order through its lifecycle. */
@Service
@RequiredArgsConstructor
public class AdminOrderService {
    private final OrderRepository orders;

    @Transactional(readOnly = true)
    public PaginationDto<OrderSummary> search(String status, String q, int page, int size) {
        OrderStatus wanted = status == null || status.isBlank() ? null : parseStatus(status);
        Specification<CustomerOrder> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (wanted != null) where.add(cb.equal(root.get("status"), wanted));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                where.add(cb.or(
                        cb.like(cb.lower(root.get("orderNumber")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"))), like),
                        cb.like(cb.lower(root.get("city")), like)));
            }
            return cb.and(where.toArray(Predicate[]::new));
        };
        return PaginationDto.of(orders.findAll(spec, PageRequests.newestFirst(page, size, 100)), OrderSummary::of);
    }

    @Transactional(readOnly = true)
    public OrderDetail get(String orderNumber) {
        return OrderDetail.of(find(orderNumber));
    }

    /** Status changes are checked against the lifecycle and recorded with who made them. */
    @Transactional
    public OrderDetail update(String orderNumber, OrderUpdate req, String staffEmail) {
        CustomerOrder o = find(orderNumber);
        if (req.status() != null && !req.status().isBlank()) {
            OrderStatus next = parseStatus(req.status());
            if (!o.getStatus().next().contains(next)) {
                throw new ConflictException("An order that is " + o.getStatus().name().toLowerCase()
                        + " cannot move to " + next.name().toLowerCase() + ".");
            }
            o.setStatus(next);
            o.record(next, Instant.now(), staffEmail, req.note() == null || req.note().isBlank() ? null : req.note().trim());
        }
        if (req.adminNote() != null) o.setAdminNote(req.adminNote().isBlank() ? null : req.adminNote().trim());
        return OrderDetail.of(o);
    }

    private CustomerOrder find(String orderNumber) {
        return orders.findByOrderNumber(orderNumber).orElseThrow(() -> new ResourceNotFoundException("Order not found."));
    }

    private static OrderStatus parseStatus(String s) {
        try {
            return OrderStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown status: " + s);
        }
    }
}
