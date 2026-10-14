package com.hollispark.store.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.client.razorpay.RazorpaySignatures;
import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.entity.order.CustomerOrder;
import com.hollispark.store.enums.OrderStatus;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.order.OrderRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final OrderRepository orders;
    private final StoreProperties.Razorpay cfg;
    private final ObjectMapper mapper;

    public PaymentService(OrderRepository orders, StoreProperties props, ObjectMapper mapper) {
        this.orders = orders;
        this.cfg = props.razorpay();
        this.mapper = mapper;
    }

    public boolean enabled() {
        return cfg.enabled();
    }

    /** The browser reports a finished payment; the signature proves Razorpay (not the browser) produced it. */
    @Transactional
    public CustomerOrder verifyFromCheckout(String orderNumber, String razorpayOrderId, String paymentId, String signature) {
        if (!enabled()) throw new ApiException(HttpStatus.CONFLICT, "Online payment is not turned on.");
        CustomerOrder o = orders.lockByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found."));
        if (o.getRazorpayOrderId() == null || !o.getRazorpayOrderId().equals(razorpayOrderId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This payment does not belong to the order.");
        }
        if (!RazorpaySignatures.validPayment(cfg.keySecret(), razorpayOrderId, paymentId, signature)) {
            log.warn("Bad Razorpay signature for order {}", orderNumber);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "The payment could not be verified. If money was taken it will be refunded.");
        }
        markPaid(o, paymentId, "customer");
        return o;
    }

    /** Razorpay calls this when a payment is captured, even if the customer closed the browser. */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (!RazorpaySignatures.validWebhook(cfg.webhookSecret(), rawBody, signature)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Bad signature.");
        }
        try {
            JsonNode root = mapper.readTree(rawBody);
            String event = root.path("event").asText();
            if (!event.equals("payment.captured") && !event.equals("order.paid")) return;
            JsonNode payment = root.at("/payload/payment/entity");
            String rzpOrderId = payment.path("order_id").asText(root.at("/payload/order/entity/id").asText(null));
            String paymentId = payment.path("id").asText(null);
            if (rzpOrderId == null || paymentId == null) return;
            orders.lockByRazorpayOrderId(rzpOrderId).ifPresentOrElse(
                    o -> markPaid(o, paymentId, "razorpay"),
                    () -> log.warn("Webhook for unknown Razorpay order {}", rzpOrderId));
        } catch (java.io.IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unreadable webhook.");
        }
    }

    private void markPaid(CustomerOrder o, String paymentId, String by) {
        if ("PAID".equals(o.getPaymentStatus())) return; // verify and webhook may both arrive
        Instant paidAt = Instant.now();
        o.setPaymentStatus("PAID");
        o.setRazorpayPaymentId(paymentId);
        o.setPaidAt(paidAt);
        if (o.getStatus() == OrderStatus.PENDING_PAYMENT) {
            o.setStatus(OrderStatus.RECEIVED);
            o.record(OrderStatus.RECEIVED, paidAt, by, "Payment received " + paymentId);
        }
    }
}
