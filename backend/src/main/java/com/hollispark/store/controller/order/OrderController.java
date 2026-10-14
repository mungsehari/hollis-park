package com.hollispark.store.controller.order;

import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.order.OrderRequest;
import com.hollispark.store.dto.order.OrderResponse;
import com.hollispark.store.dto.order.PaymentConfirmation;
import com.hollispark.store.dto.order.PaymentResult;
import com.hollispark.store.service.order.OrderService;
import com.hollispark.store.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Checkout", description = "Placing orders and confirming payment")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orders;
    private final PaymentService payments;

    @Operation(summary = "Place an order. Prices, stock and discounts are recomputed on the server")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> place(@Valid @RequestBody OrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orders.place(req), HttpStatus.CREATED.value(), "Order placed"));
    }

    @Operation(summary = "Confirm a Razorpay payment; the signature is verified on the server")
    @PostMapping("/{orderNumber}/payment")
    public ResponseEntity<ApiResponse<PaymentResult>> confirmPayment(@PathVariable String orderNumber,
                                                                     @Valid @RequestBody PaymentConfirmation req) {
        var o = payments.verifyFromCheckout(orderNumber, req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        return ResponseEntity.ok(ApiResponse.success(new PaymentResult(o.getOrderNumber(), o.getStatus().name(), o.getPaymentStatus()), "Payment received"));
    }
}
