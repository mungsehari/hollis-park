package com.hollispark.store.web;

import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.ContactMessage;
import com.hollispark.store.domain.ContactRepository;
import com.hollispark.store.domain.PromoCodeRepository;
import com.hollispark.store.domain.SubscriberRepository;
import com.hollispark.store.domain.Subscriber;
import com.hollispark.store.dto.OrderRequest;
import com.hollispark.store.dto.OrderResponse;
import com.hollispark.store.dto.Requests.ContactRequest;
import com.hollispark.store.dto.Requests.ContactResponse;
import com.hollispark.store.dto.Requests.NewsletterRequest;
import com.hollispark.store.dto.Requests.NewsletterResponse;
import com.hollispark.store.dto.Requests.PromoRequest;
import com.hollispark.store.dto.Requests.PromoResponse;
import com.hollispark.store.dto.Requests.PaymentConfirmation;
import com.hollispark.store.dto.Requests.PaymentResult;
import com.hollispark.store.payment.PaymentService;
import com.hollispark.store.service.ApiException;
import com.hollispark.store.service.OrderService;
import com.hollispark.store.service.SettingsService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
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
    private final ContactRepository contacts;
    private final SubscriberRepository subscribers;
    private final SettingsService settings;
    private final PromoCodeRepository promos;
    private final PaymentService payments;

    public CheckoutController(OrderService orders, StoreProperties props, ContactRepository contacts,
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
        return new PaymentResult(o.orderNumber, o.status.name(), o.paymentStatus);
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse contact(@Valid @RequestBody ContactRequest req) {
        if ("order".equals(req.topic()) && (req.order() == null || !req.order().matches(props.orderPrefix() + "[0-9]{5}"))) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Order numbers look like " + props.orderPrefix() + "24817.");
        }
        ContactMessage m = new ContactMessage();
        m.name = req.name().trim();
        m.email = req.email().trim().toLowerCase();
        m.topic = req.topic();
        m.orderNumber = req.order();
        m.slot = req.slot();
        m.message = req.message().trim();
        m.status = "NEW";
        m.createdAt = Instant.now();
        contacts.save(m);
        return new ContactResponse("received");
    }

    @PostMapping("/newsletter")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsletterResponse subscribe(@Valid @RequestBody NewsletterRequest req) {
        String email = req.email().trim().toLowerCase();
        Subscriber s = subscribers.findByEmail(email).orElseGet(Subscriber::new);
        s.email = email;
        s.topics = req.topics() == null ? List.of() : req.topics();
        if (s.createdAt == null) s.createdAt = Instant.now();
        subscribers.save(s);
        var first = promos.findFirstByActiveTrueOrderByCreatedAtAsc().orElse(null);
        return new NewsletterResponse("subscribed", first == null ? null : first.code, first == null ? null : first.rate);
    }
}
