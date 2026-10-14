package com.hollispark.store.service;

import com.hollispark.store.enums.ShipMethod;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** The one place that turns a subtotal into delivery, discount, tax and total. Mirrors the storefront's maths. */
@Service
public class PricingService {
    private final SettingsService settings;

    public PricingService(SettingsService settings) {
        this.settings = settings;
    }

    public record Quote(BigDecimal subtotal, BigDecimal discount, BigDecimal shipping, BigDecimal tax, BigDecimal total) {}

    public Quote quote(BigDecimal subtotal, ShipMethod ship, BigDecimal promoRate) {
        return quote(settings.get(), subtotal, ship, promoRate);
    }

    public static Quote quote(SettingsService.Settings s, BigDecimal subtotal, ShipMethod ship, BigDecimal promoRate) {
        BigDecimal discount = promoRate == null ? BigDecimal.ZERO : money(subtotal.multiply(promoRate));
        BigDecimal shipping = switch (ship) {
            case EXPRESS -> BigDecimal.valueOf(s.expressFee());
            case PICKUP -> BigDecimal.ZERO;
            case STANDARD -> subtotal.compareTo(BigDecimal.valueOf(s.freeShipFrom())) >= 0
                    ? BigDecimal.ZERO : BigDecimal.valueOf(s.shipFee());
        };
        BigDecimal gross = money(subtotal.subtract(discount).add(shipping));
        BigDecimal rate = s.taxRate() == null ? BigDecimal.ZERO : s.taxRate();
        BigDecimal tax = BigDecimal.ZERO;
        if (rate.signum() > 0) {
            tax = s.taxIncluded()
                    ? money(gross.multiply(rate).divide(BigDecimal.ONE.add(rate), 10, RoundingMode.HALF_UP))
                    : money(subtotal.subtract(discount).multiply(rate));
        }
        BigDecimal total = s.taxIncluded() ? gross : money(gross.add(tax));
        return new Quote(money(subtotal), discount, money(shipping), tax, total);
    }

    private static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
