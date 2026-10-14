package com.hollispark.store.dto.marketing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public record PromoUpdate(Boolean active, @DecimalMin(value = "0.01") @DecimalMax("0.9") BigDecimal rate) {}
