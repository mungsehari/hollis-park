package com.hollispark.store.repository.content;

import com.hollispark.store.entity.content.SiteContent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteContentRepository extends JpaRepository<SiteContent, UUID> {
    Optional<SiteContent> findByPageKey(String pageKey);

    boolean existsByPageKey(String pageKey);
}
