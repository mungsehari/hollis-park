package com.hollispark.store.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarePickRepository extends JpaRepository<CarePick, UUID> {
    List<CarePick> findAllByOrderBySortOrderAscCreatedAtAsc();

    List<CarePick> findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    Optional<CarePick> findByIdAndActiveTrue(UUID id);
}
