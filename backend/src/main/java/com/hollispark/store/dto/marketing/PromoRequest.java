package com.hollispark.store.dto.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromoRequest(@NotBlank @Size(max = 40) String code) {}
