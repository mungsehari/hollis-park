package com.hollispark.store.dto.marketing;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record NewsletterRequest(
        @NotBlank @Email @Size(max = 200) String email,
        @Size(max = 10) List<@Size(max = 30) String> topics) {}
