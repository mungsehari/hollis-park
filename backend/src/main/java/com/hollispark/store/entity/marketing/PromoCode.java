package com.hollispark.store.entity.marketing;

import com.hollispark.store.entity.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class PromoCode extends BaseEntity {
    /** Always stored upper case. */
    @Column(nullable = false, unique = true, updatable = false)
    private String code;
    private BigDecimal rate;
    @Builder.Default
    private boolean active = true;
}
