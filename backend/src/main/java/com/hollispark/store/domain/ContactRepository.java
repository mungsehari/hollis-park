package com.hollispark.store.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<ContactMessage, Long> {
    Page<ContactMessage> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
