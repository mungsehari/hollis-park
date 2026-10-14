package com.hollispark.store.dto.marketing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record PromoCreate(@NotBlank @Pattern(regexp = "[A-Za-z0-9]{3,40}", message = "Use 3-40 letters or digits.") String code,
                          @NotNull @DecimalMin(value = "0.01") @DecimalMax("0.9") BigDecimal rate) {}
