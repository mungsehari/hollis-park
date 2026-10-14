package com.hollispark.store.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordReset(@NotBlank @Size(min = 10, max = 200) String password) {}
