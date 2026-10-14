package com.hollispark.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.hollispark.store.enums.Marketplace;
import org.junit.jupiter.api.Test;

class MarketplaceTest {
    @Test
    void acceptsOnlyHttpsLinksOnTheMarketplacesOwnDomains() {
        assertThat(Marketplace.AMAZON.accepts("https://www.amazon.in/dp/B0TEST?tag=hollis-21")).isTrue();
        assertThat(Marketplace.AMAZON.accepts("https://amzn.to/3abcd")).isTrue();
        assertThat(Marketplace.FLIPKART.accepts("https://www.flipkart.com/p/itm123")).isTrue();
        assertThat(Marketplace.FLIPKART.accepts("https://dl.flipkart.com/s/abc")).isTrue();

        assertThat(Marketplace.AMAZON.accepts("http://www.amazon.in/dp/B0TEST")).isFalse();          // not https
        assertThat(Marketplace.AMAZON.accepts("https://amazon.in.evil.com/dp/x")).isFalse();         // look-alike host
        assertThat(Marketplace.AMAZON.accepts("https://evilamazon.in/x")).isFalse();
        assertThat(Marketplace.AMAZON.accepts("https://user@www.amazon.in/x")).isFalse();            // credentials trick
        assertThat(Marketplace.AMAZON.accepts("https://www.flipkart.com/p/1")).isFalse();            // other marketplace
        assertThat(Marketplace.FLIPKART.accepts("javascript:alert(1)")).isFalse();
        assertThat(Marketplace.FLIPKART.accepts("")).isFalse();
        assertThat(Marketplace.parse("amazon")).isEqualTo(Marketplace.AMAZON);
        assertThat(Marketplace.parse("ebay")).isNull();
    }
}
