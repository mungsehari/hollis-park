package com.hollispark.store.repository.catalog;

import com.hollispark.store.entity.catalog.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByProductSlugOrderBySortOrderAsc(String productSlug);

    List<Review> findAllByOrderBySortOrderAsc();
}
