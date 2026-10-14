package com.hollispark.store.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChange(@NotBlank String current, @NotBlank @Size(min = 10, max = 200) String next) {}
