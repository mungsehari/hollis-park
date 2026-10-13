package com.hollispark.store.domain;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Where a customer can be sent to buy. Links are only accepted for the marketplace's own https hosts,
 * so the store's redirect can never be turned into a link to somewhere else.
 */
public enum Marketplace {
    AMAZON("Amazon", Set.of("amazon.in", "amazon.com", "amzn.to", "amzn.in", "amzn.eu")),
    FLIPKART("Flipkart", Set.of("flipkart.com", "fkrt.it", "fkrt.cc", "fktr.in"));

    private final String label;
    private final Set<String> hosts;

    Marketplace(String label, Set<String> hosts) {
        this.label = label;
        this.hosts = hosts;
    }

    public String label() {
        return label;
    }

    /** True for an https URL on one of this marketplace's domains (sub-domains included, e.g. www.amazon.in). */
    public boolean accepts(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URI u = URI.create(url.trim());
            if (!"https".equalsIgnoreCase(u.getScheme()) || u.getHost() == null || u.getUserInfo() != null) return false;
            String host = u.getHost().toLowerCase(Locale.ROOT);
            return hosts.stream().anyMatch(h -> host.equals(h) || host.endsWith("." + h));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** Case-insensitive lookup for path variables ("amazon"); null when unknown. */
    public static Marketplace parse(String s) {
        for (Marketplace m : values()) if (m.name().equalsIgnoreCase(s)) return m;
        return null;
    }
}
