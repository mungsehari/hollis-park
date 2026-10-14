package com.hollispark.store.controller.order;

import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.dto.marketing.ContactRequest;
import com.hollispark.store.dto.marketing.ContactResponse;
import com.hollispark.store.dto.marketing.NewsletterRequest;
import com.hollispark.store.dto.marketing.NewsletterResponse;
import com.hollispark.store.dto.marketing.PromoRequest;
import com.hollispark.store.dto.marketing.PromoResponse;
import com.hollispark.store.dto.order.OrderRequest;
import com.hollispark.store.dto.order.OrderResponse;
import com.hollispark.store.dto.order.PaymentConfirmation;
import com.hollispark.store.dto.order.PaymentResult;
import com.hollispark.store.entity.marketing.ContactMessage;
import com.hollispark.store.entity.marketing.Subscriber;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.marketing.ContactMessageRepository;
import com.hollispark.store.repository.marketing.PromoCodeRepository;
import com.hollispark.store.repository.marketing.SubscriberRepository;
import com.hollispark.store.service.order.OrderService;
import com.hollispark.store.service.payment.PaymentService;
import com.hollispark.store.service.settings.SettingsService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckoutController {
    private final OrderService orders;
    private final StoreProperties props;
    private final ContactMessageRepository contacts;
    private final SubscriberRepository subscribers;
    private final SettingsService settings;
    private final PromoCodeRepository promos;
    private final PaymentService payments;

    public CheckoutController(OrderService orders, StoreProperties props, ContactMessageRepository contacts,
                              SubscriberRepository subscribers, SettingsService settings, PromoCodeRepository promos,
                              PaymentService payments) {
        this.payments = payments;
        this.orders = orders;
        this.props = props;
        this.contacts = contacts;
        this.subscribers = subscribers;
        this.settings = settings;
        this.promos = promos;
    }

    @PostMapping("/promo/validate")
    public PromoResponse validatePromo(@Valid @RequestBody PromoRequest req) {
        BigDecimal rate = settings.promoRate(req.code());
        if (rate == null) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "That code is not valid.");
        return new PromoResponse(req.code().trim().toUpperCase(), rate);
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest req) {
        return orders.place(req);
    }

    /** Called by the store after Razorpay Checkout succeeds; the signature is verified server-side. */
    @PostMapping("/orders/{orderNumber}/payment")
    public PaymentResult confirmPayment(@PathVariable String orderNumber, @Valid @RequestBody PaymentConfirmation req) {
        var o = payments.verifyFromCheckout(orderNumber, req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        return new PaymentResult(o.getOrderNumber(), o.getStatus().name(), o.getPaymentStatus());
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse contact(@Valid @RequestBody ContactRequest req) {
        if ("order".equals(req.topic()) && (req.order() == null || !req.order().matches(props.orderPrefix() + "[0-9]{5}"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Order numbers look like " + props.orderPrefix() + "24817.");
        }
        contacts.save(ContactMessage.builder()
                .name(req.name().trim())
                .email(req.email().trim().toLowerCase())
                .topic(req.topic())
                .orderNumber(req.order())
                .slot(req.slot())
                .message(req.message().trim())
                .build());
        return new ContactResponse("received");
    }

    @PostMapping("/newsletter")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsletterResponse subscribe(@Valid @RequestBody NewsletterRequest req) {
        String email = req.email().trim().toLowerCase();
        Subscriber s = subscribers.findByEmail(email).orElseGet(() -> Subscriber.builder().email(email).build());
        s.setTopics(req.topics() == null ? List.of() : req.topics());
        subscribers.save(s);
        var first = promos.findFirstByActiveTrueOrderByCreatedAtAsc().orElse(null);
        return new NewsletterResponse("subscribed", first == null ? null : first.getCode(),
                first == null ? null : first.getRate());
    }
}
