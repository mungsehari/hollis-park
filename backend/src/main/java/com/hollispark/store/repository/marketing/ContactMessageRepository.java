package com.hollispark.store.repository.marketing;

import com.hollispark.store.entity.marketing.ContactMessage;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, UUID> {
    Page<ContactMessage> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
