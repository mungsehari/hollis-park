package com.hollispark.store.dto.marketing;

import com.hollispark.store.entity.marketing.Subscriber;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubscriberDto(UUID id, String email, List<String> topics, Instant createdAt) {
    public static SubscriberDto of(Subscriber s) {
        return new SubscriberDto(s.getId(), s.getEmail(), s.getTopics(), s.getCreatedAt());
    }
}
