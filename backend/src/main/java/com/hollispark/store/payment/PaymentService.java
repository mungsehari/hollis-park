package com.hollispark.store.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.CustomerOrder;
import com.hollispark.store.domain.OrderRepository;
import com.hollispark.store.domain.OrderStatus;
import com.hollispark.store.domain.StatusEvent;
import com.hollispark.store.service.ApiException;
import java.time.Instant;
import java.util.ArrayList;
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
        CustomerOrder o = orders.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found."));
        if (o.razorpayOrderId == null || !o.razorpayOrderId.equals(razorpayOrderId)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "This payment does not belong to the order.");
        }
        if (!Signatures.validPayment(cfg.keySecret(), razorpayOrderId, paymentId, signature)) {
            log.warn("Bad Razorpay signature for order {}", orderNumber);
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "The payment could not be verified. If money was taken it will be refunded.");
        }
        markPaid(o, paymentId, "customer");
        return o;
    }

    /** Razorpay calls this when a payment is captured, even if the customer closed the browser. */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        if (!Signatures.validWebhook(cfg.webhookSecret(), rawBody, signature)) {
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
            orders.findByRazorpayOrderId(rzpOrderId).ifPresentOrElse(
                    o -> markPaid(o, paymentId, "razorpay"),
                    () -> log.warn("Webhook for unknown Razorpay order {}", rzpOrderId));
        } catch (java.io.IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unreadable webhook.");
        }
    }

    private void markPaid(CustomerOrder o, String paymentId, String by) {
        if ("PAID".equals(o.paymentStatus)) return; // verify and webhook may both arrive
        o.paymentStatus = "PAID";
        o.razorpayPaymentId = paymentId;
        o.paidAt = Instant.now();
        if (o.status == OrderStatus.PENDING_PAYMENT) {
            o.status = OrderStatus.RECEIVED;
            var history = new ArrayList<>(o.history == null ? java.util.List.<StatusEvent>of() : o.history);
            history.add(new StatusEvent(OrderStatus.RECEIVED.name(), o.paidAt, by, "Payment received " + paymentId));
            o.history = history;
        }
        o.updatedAt = o.paidAt;
    }
}
