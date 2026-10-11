package com.hollispark.store.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeRepository extends JpaRepository<PromoCode, String> {
    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);

    List<PromoCode> findAllByOrderByCreatedAtAsc();

    Optional<PromoCode> findFirstByActiveTrueOrderByCreatedAtAsc();
}
