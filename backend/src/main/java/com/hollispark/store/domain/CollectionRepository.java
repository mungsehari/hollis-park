package com.hollispark.store.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<CollectionEntity, String> {
    List<CollectionEntity> findAllByOrderBySortOrderAsc();

    List<CollectionEntity> findAllByActiveTrueOrderBySortOrderAsc();
}
