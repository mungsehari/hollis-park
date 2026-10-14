package com.hollispark.store.dto.marketing;

import com.hollispark.store.domain.ContactMessage;
import java.time.Instant;
import java.util.UUID;

public record MessageDto(UUID id, String name, String email, String topic, String orderNumber, String slot,
                         String message, String status, Instant createdAt) {
    public static MessageDto of(ContactMessage m) {
        return new MessageDto(m.getId(), m.getName(), m.getEmail(), m.getTopic(), m.getOrderNumber(), m.getSlot(),
                m.getMessage(), m.getStatus(), m.getCreatedAt());
    }
}
