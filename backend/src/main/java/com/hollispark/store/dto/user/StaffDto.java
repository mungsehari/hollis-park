package com.hollispark.store.dto.user;

import com.hollispark.store.entity.user.StaffUser;
import java.time.Instant;
import java.util.UUID;

public record StaffDto(UUID id, String email, String name, String role, boolean active, Instant createdAt, Instant lastLoginAt) {
    public static StaffDto of(StaffUser u) {
        return new StaffDto(u.getId(), u.getEmail(), u.getName(), u.getRole().name(), u.isActive(), u.getCreatedAt(),
                u.getLastLoginAt());
    }
}
