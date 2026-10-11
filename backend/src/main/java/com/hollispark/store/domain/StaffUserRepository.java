package com.hollispark.store.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {
    Optional<StaffUser> findByEmailIgnoreCase(String email);

    long countByRoleAndActiveTrue(StaffUser.Role role);
}
