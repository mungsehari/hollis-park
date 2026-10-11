package com.hollispark.store.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
    Optional<Media> findByStorageKey(String storageKey);
}
