package com.hollispark.store.dto.base;

import jakarta.validation.constraints.NotNull;

public record ActiveUpdate(@NotNull Boolean active) {}
