package com.hollispark.store.repository.content;

import com.hollispark.store.entity.content.CarePick;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarePickRepository extends JpaRepository<CarePick, UUID> {
    List<CarePick> findAllByOrderBySortOrderAscCreatedAtAsc();

    List<CarePick> findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    Optional<CarePick> findByIdAndActiveTrue(UUID id);
}
