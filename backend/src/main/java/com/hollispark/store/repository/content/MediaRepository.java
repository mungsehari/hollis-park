package com.hollispark.store.repository.content;

import com.hollispark.store.entity.content.Media;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, UUID> {
    Optional<Media> findByStorageKey(String storageKey);
}
