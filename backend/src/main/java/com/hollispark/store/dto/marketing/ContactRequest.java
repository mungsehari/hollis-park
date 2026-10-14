package com.hollispark.store.dto.marketing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(max = 40) String topic,
        @Size(max = 40) String order,
        @Size(max = 80) String slot,
        @NotBlank @Size(min = 10, max = 4000) String message) {}
