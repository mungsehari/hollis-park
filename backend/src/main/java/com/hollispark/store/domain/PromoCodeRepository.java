package com.hollispark.store.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {
    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<PromoCode> findAllByOrderByCreatedAtAsc();

    Optional<PromoCode> findFirstByActiveTrueOrderByCreatedAtAsc();
}
