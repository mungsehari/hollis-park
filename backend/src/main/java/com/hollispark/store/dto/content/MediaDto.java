package com.hollispark.store.dto.content;

import com.hollispark.store.entity.content.Media;
import java.time.Instant;
import java.util.UUID;

public record MediaDto(UUID id, String url, String originalName, String contentType, long sizeBytes, String alt, Instant createdAt) {
    public static MediaDto of(Media m) {
        return new MediaDto(m.getId(), m.getUrl(), m.getOriginalName(), m.getContentType(), m.getSizeBytes(), m.getAlt(),
                m.getCreatedAt());
    }
}
