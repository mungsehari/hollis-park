package com.hollispark.store.service;

import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.CustomerOrder;
import com.hollispark.store.domain.OrderLine;
import com.hollispark.store.domain.OrderRepository;
import com.hollispark.store.domain.OrderStatus;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.StatusEvent;
import com.hollispark.store.domain.Product;
import com.hollispark.store.domain.OrderRepository;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.dto.OrderRequest;
import com.hollispark.store.dto.OrderRequest.Width;
import com.hollispark.store.dto.OrderResponse;
import com.hollispark.store.payment.RazorpayClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private static final int ORDER_NUMBER_BASE = 24000;

    private final ProductRepository products;
    private final OrderRepository orders;
    private final PricingService pricing;
    private final StoreProperties props;
    private final StaticContent content;
    private final SettingsService settings;
    private final RazorpayClient razorpay;

    public OrderService(ProductRepository products, OrderRepository orders, PricingService pricing,
                        StoreProperties props, StaticContent content, SettingsService settings, RazorpayClient razorpay) {
        this.razorpay = razorpay;
        this.products = products;
        this.orders = orders;
        this.pricing = pricing;
        this.props = props;
        this.content = content;
        this.settings = settings;
    }

    /**
     * Prices, stock and the discount are always re-derived from the database; the browser only
     * says what it wants. No payment is taken: the order is stored as RECEIVED.
     */
    @Transactional
    public OrderResponse place(OrderRequest req) {
        BigDecimal rate = null;
        String promo = null;
        if (req.promo() != null && !req.promo().isBlank()) {
            rate = settings.promoRate(req.promo());
            if (rate == null) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "That discount code is not valid.");
            promo = req.promo().trim().toUpperCase();
        }

        Map<String, Product> byId = products.findAllById(
                        req.items().stream().map(OrderRequest.Item::id).distinct().toList())
                .stream().collect(Collectors.toMap(p -> p.id, Function.identity()));

        List<OrderLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderRequest.Item item : req.items()) {
            Product p = byId.get(item.id());
            if (p == null || !p.active) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown product: " + item.id());
            boolean wide = item.width() == Width.WIDE;
            if (!isKnownSize(item.size())) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Unknown size: " + trim(item.size()));
            }
            if (wide && p.wide == null) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, p.name + " is not available in Wide.");
            }
            if (!p.inStock(item.size(), wide)) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        p.name + " in " + props.sizeLabel() + " " + trim(item.size()) + (wide ? " Wide" : "") + " is sold out.");
            }
            BigDecimal unit = BigDecimal.valueOf(p.unitPrice(wide));
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(item.qty()));
            subtotal = subtotal.add(lineTotal);
            lines.add(new OrderLine(p.id, p.name, item.size(), wide ? "wide" : "standard", item.qty(), unit, lineTotal, p.img));
        }

        PricingService.Quote q = pricing.quote(subtotal, req.ship(), rate);

        CustomerOrder o = new CustomerOrder();
        o.email = req.email().trim().toLowerCase();
        o.first = req.first().trim();
        o.last = req.last().trim();
        o.address = req.address().trim();
        o.city = req.city().trim();
        o.pin = req.pin();
        o.ship = req.ship().name().toLowerCase();
        o.promo = promo;
        o.subtotal = q.subtotal();
        o.discount = q.discount();
        o.shipping = q.shipping();
        o.tax = q.tax();
        o.total = q.total();
        boolean online = props.razorpay().enabled();
        o.status = online ? OrderStatus.PENDING_PAYMENT : OrderStatus.RECEIVED;
        o.paymentStatus = online ? "PENDING" : "NONE";
        o.createdAt = Instant.now();
        o.updatedAt = o.createdAt;
        o.history = new ArrayList<>(List.of(new StatusEvent(o.status.name(), o.createdAt, "customer", null)));
        o.items = lines;
        orders.save(o);
        o.orderNumber = props.orderPrefix() + String.format("%05d", ORDER_NUMBER_BASE + o.id);

        OrderResponse.Payment payment = null;
        if (online) {
            // Razorpay amounts are in paise. A failure here rolls the whole order back, so nothing is left half-made.
            long paise = q.total().movePointRight(2).longValueExact();
            var rzp = razorpay.createOrder(paise, props.currency(), o.orderNumber);
            o.razorpayOrderId = rzp.id();
            payment = new OrderResponse.Payment(props.razorpay().keyId(), rzp.id(), rzp.amountPaise(), rzp.currency(),
                    props.razorpay().testMode());
        }

        return new OrderResponse(o.orderNumber, o.status.name(), o.paymentStatus, q.subtotal(), q.discount(),
                q.shipping(), q.tax(), q.total(), o.ship, payment);
    }

    private boolean isKnownSize(double size) {
        for (var n : content.sizes) if (n.asDouble() == size) return true;
        return false;
    }

    private static String trim(double size) {
        return size == Math.rint(size) ? String.valueOf((long) size) : String.valueOf(size);
    }
}
