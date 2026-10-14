package com.hollispark.store.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StaffUpdate(@Size(max = 120) String name, @Pattern(regexp = "ADMIN|STAFF") String role, Boolean active) {}
