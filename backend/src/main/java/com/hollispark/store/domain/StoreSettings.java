package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** Single-row table (the database allows only one row): tax and delivery settings edited in the admin panel. */
@Entity
@Table(name = "store_settings")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class StoreSettings extends BaseEntity {
    @Column(name = "tax_mode")
    private String taxMode;
    @Column(name = "tax_rate")
    private BigDecimal taxRate;
    @Column(name = "tax_name")
    private String taxName;
    @Column(name = "free_ship_from")
    private int freeShipFrom;
    @Column(name = "ship_fee")
    private int shipFee;
    @Column(name = "express_fee")
    private int expressFee;
}
