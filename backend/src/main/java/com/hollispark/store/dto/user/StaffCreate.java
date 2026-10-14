package com.hollispark.store.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffCreate(@NotBlank @Email @Size(max = 200) String email, @NotBlank @Size(max = 120) String name,
                          @NotBlank @Pattern(regexp = "ADMIN|STAFF") String role,
                          @NotBlank @Size(min = 10, max = 200) String password) {}
