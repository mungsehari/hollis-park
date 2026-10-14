package com.hollispark.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.hollispark.store.enums.ShipMethod;
import com.hollispark.store.service.PricingService;
import com.hollispark.store.service.SettingsService.Settings;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

    private record P(Settings s) {
        PricingService.Quote quote(BigDecimal subtotal, ShipMethod ship, BigDecimal rate) {
            return PricingService.quote(s, subtotal, ship, rate);
        }
    }

    private static P pricing(String taxMode, String taxRate) {
        return new P(new Settings(taxMode, new BigDecimal(taxRate), "GST", 12000, 149, 349));
    }

    @Test
    void standardDeliveryChargedBelowThreshold() {
        var q = pricing("included", "0").quote(new BigDecimal("10999"), ShipMethod.STANDARD, null);
        assertThat(q.shipping()).isEqualByComparingTo("149");
        assertThat(q.total()).isEqualByComparingTo("11148");
    }

    @Test
    void standardDeliveryFreeAtThreshold() {
        var q = pricing("included", "0").quote(new BigDecimal("12000"), ShipMethod.STANDARD, null);
        assertThat(q.shipping()).isEqualByComparingTo("0");
        assertThat(q.total()).isEqualByComparingTo("12000");
    }

    @Test
    void expressAndPickup() {
        var p = pricing("included", "0");
        assertThat(p.quote(new BigDecimal("20000"), ShipMethod.EXPRESS, null).shipping()).isEqualByComparingTo("349");
        assertThat(p.quote(new BigDecimal("100"), ShipMethod.PICKUP, null).shipping()).isEqualByComparingTo("0");
    }

    @Test
    void promoDiscountsTheSubtotal() {
        var q = pricing("included", "0").quote(new BigDecimal("12999"), ShipMethod.STANDARD, new BigDecimal("0.10"));
        assertThat(q.discount()).isEqualByComparingTo("1299.90");
        assertThat(q.total()).isEqualByComparingTo("11699.10");
    }

    @Test
    void includedTaxIsShownButNotAdded() {
        var q = pricing("included", "0.18").quote(new BigDecimal("11800"), ShipMethod.PICKUP, null);
        assertThat(q.tax()).isEqualByComparingTo("1800.00");
        assertThat(q.total()).isEqualByComparingTo("11800");
    }

    @Test
    void addedTaxIsAddedToTotal() {
        var q = pricing("added", "0.10").quote(new BigDecimal("1000"), ShipMethod.PICKUP, null);
        assertThat(q.tax()).isEqualByComparingTo("100");
        assertThat(q.total()).isEqualByComparingTo("1100");
    }
}
