package com.hollispark.store.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreSettingsRepository extends JpaRepository<StoreSettings, UUID> {
    /** The table holds exactly one row. */
    Optional<StoreSettings> findFirstByOrderByCreatedAtAsc();
}
