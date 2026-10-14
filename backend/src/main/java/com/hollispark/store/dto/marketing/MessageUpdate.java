package com.hollispark.store.dto.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MessageUpdate(@NotBlank @Pattern(regexp = "NEW|HANDLED") String status) {}
