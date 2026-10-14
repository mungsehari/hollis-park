package com.hollispark.store.dto.marketing;

import com.hollispark.store.entity.marketing.PromoCode;
import java.math.BigDecimal;
import java.time.Instant;

public record PromoDto(String code, BigDecimal rate, boolean active, Instant createdAt) {
    public static PromoDto of(PromoCode p) {
        return new PromoDto(p.getCode(), p.getRate(), p.isActive(), p.getCreatedAt());
    }
}
