package com.hollispark.store.dto.settings;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SettingsDto(@NotBlank @Pattern(regexp = "included|added") String taxMode,
                          @NotNull @DecimalMin("0") @DecimalMax("0.9999") BigDecimal taxRate,
                          @NotBlank @Size(max = 40) String taxName,
                          @Min(0) @Max(10_000_000) int freeShipFrom,
                          @Min(0) @Max(1_000_000) int shipFee,
                          @Min(0) @Max(1_000_000) int expressFee) {}
