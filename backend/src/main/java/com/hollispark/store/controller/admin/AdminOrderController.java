package com.hollispark.store.controller.admin;

import com.hollispark.store.dto.base.PageDto;
import com.hollispark.store.dto.order.OrderDetail;
import com.hollispark.store.dto.order.OrderSummary;
import com.hollispark.store.dto.order.OrderUpdate;
import com.hollispark.store.entity.order.CustomerOrder;
import com.hollispark.store.enums.OrderStatus;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.order.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderRepository orders;

    public AdminOrderController(OrderRepository orders) {
        this.orders = orders;
    }

    @GetMapping
    public PageDto<OrderSummary> list(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        Specification<CustomerOrder> spec = (root, query, cb) -> {
            List<Predicate> where = new ArrayList<>();
            if (status != null && !status.isBlank()) where.add(cb.equal(root.get("status"), parseStatus(status)));
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
        var pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageDto.of(orders.findAll(spec, pageable).map(OrderSummary::of));
    }

    @GetMapping("/{orderNumber}")
    public OrderDetail get(@PathVariable String orderNumber) {
        return OrderDetail.of(find(orderNumber));
    }

    @PatchMapping("/{orderNumber}")
    @Transactional
    public OrderDetail update(@PathVariable String orderNumber, @Valid @RequestBody OrderUpdate req, Principal who) {
        CustomerOrder o = find(orderNumber);
        if (req.status() != null && !req.status().isBlank()) {
            OrderStatus next = parseStatus(req.status());
            if (!o.getStatus().next().contains(next)) {
                throw new ApiException(HttpStatus.CONFLICT, "An order that is " + o.getStatus().name().toLowerCase()
                        + " cannot move to " + next.name().toLowerCase() + ".");
            }
            o.setStatus(next);
            o.record(next, Instant.now(), who.getName(),
                    req.note() == null || req.note().isBlank() ? null : req.note().trim());
        }
        if (req.adminNote() != null) o.setAdminNote(req.adminNote().isBlank() ? null : req.adminNote().trim());
        return OrderDetail.of(o);
    }

    private CustomerOrder find(String orderNumber) {
        return orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found."));
    }

    private static OrderStatus parseStatus(String s) {
        try {
            return OrderStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown status: " + s);
        }
    }
}
