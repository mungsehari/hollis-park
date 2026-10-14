package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.base.PaginationDto;
import com.hollispark.store.dto.order.OrderDetail;
import com.hollispark.store.dto.order.OrderSummary;
import com.hollispark.store.dto.order.OrderUpdate;
import com.hollispark.store.service.order.AdminOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: orders")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminOrderController {
    private final AdminOrderService orders;

    @Operation(summary = "Search orders by status and/or text (number, email, name, city), newest first")
    @GetMapping
    public ResponseEntity<ApiResponse<PaginationDto<OrderSummary>>> list(@RequestParam(required = false) String status,
                                                                         @RequestParam(required = false) String q,
                                                                         @RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(orders.search(status, q, page, size)));
    }

    @Operation(summary = "One order with its history")
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDetail>> get(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success(orders.get(orderNumber)));
    }

    @Operation(summary = "Move the order along its lifecycle and/or set the internal note")
    @PatchMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDetail>> update(@PathVariable String orderNumber, @Valid @RequestBody OrderUpdate req,
                                                           Principal who) {
        return ResponseEntity.ok(ApiResponse.success(orders.update(orderNumber, req, who.getName()), "Order updated"));
    }
}
