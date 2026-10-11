package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "staff_users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class StaffUser extends BaseEntity {
    public enum Role { ADMIN, STAFF }

    private String email;
    private String name;
    @Column(name = "password_hash")
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    private Role role;
    @Builder.Default
    private boolean active = true;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
