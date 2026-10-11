package com.hollispark.store.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findAllByOrderByRankAsc();

    List<Product> findAllByActiveTrueOrderByRankAsc();

    Optional<Product> findByIdAndActiveTrue(String id);
}
