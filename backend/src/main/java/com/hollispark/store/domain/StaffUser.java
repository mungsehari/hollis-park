package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "staff_users")
public class StaffUser {
    public enum Role { ADMIN, STAFF }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String email;
    public String name;
    @Column(name = "password_hash")
    public String passwordHash;
    @Enumerated(EnumType.STRING)
    public Role role;
    public boolean active = true;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "last_login_at")
    public Instant lastLoginAt;
}
