package com.hollispark.store.config;

/**
 * The two {@code @PreAuthorize} rules the admin API uses. STAFF works orders and messages and can read
 * everything else; ADMIN can also change the catalog, content, settings, promo codes and staff.
 */
public final class Access {
    private Access() {}

    public static final String STAFF = "hasAnyRole('ADMIN','STAFF')";
    public static final String ADMIN = "hasRole('ADMIN')";
}
