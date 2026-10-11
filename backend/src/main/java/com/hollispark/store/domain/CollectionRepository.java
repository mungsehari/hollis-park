package com.hollispark.store.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<CollectionEntity, UUID> {
    List<CollectionEntity> findAllByOrderBySortOrderAsc();

    List<CollectionEntity> findAllByActiveTrueOrderBySortOrderAsc();

    Optional<CollectionEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
