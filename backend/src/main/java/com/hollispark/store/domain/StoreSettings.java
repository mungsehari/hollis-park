package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Single-row table (id = 1): tax and delivery settings edited in the admin panel. */
@Entity
@Table(name = "store_settings")
public class StoreSettings {
    @Id
    public Integer id = 1;
    @Column(name = "tax_mode")
    public String taxMode;
    @Column(name = "tax_rate")
    public BigDecimal taxRate;
    @Column(name = "tax_name")
    public String taxName;
    @Column(name = "free_ship_from")
    public int freeShipFrom;
    @Column(name = "ship_fee")
    public int shipFee;
    @Column(name = "express_fee")
    public int expressFee;
    @Column(name = "updated_at")
    public Instant updatedAt;
}
