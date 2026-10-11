package com.hollispark.store.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, UUID> {
    Optional<StaffUser> findByEmailIgnoreCase(String email);

    long countByRoleAndActiveTrue(StaffUser.Role role);
}
