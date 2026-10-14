package com.hollispark.store.dto.auth;

import com.hollispark.store.domain.StaffUser;
import java.util.UUID;

public record Me(UUID id, String email, String name, String role) {
    public static Me of(StaffUser u) {
        return new Me(u.getId(), u.getEmail(), u.getName(), u.getRole().name());
    }
}
