package com.hollispark.store.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findAllByOrderBySortRankAsc();

    List<Product> findAllByActiveTrueOrderBySortRankAsc();

    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugAndActiveTrue(String slug);

    List<Product> findAllBySlugIn(Collection<String> slugs);

    boolean existsBySlug(String slug);

    long countByActiveTrue();
}
