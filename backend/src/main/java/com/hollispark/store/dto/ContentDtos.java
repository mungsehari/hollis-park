package com.hollispark.store.dto;

import com.hollispark.store.domain.CollectionEntity;
import com.hollispark.store.domain.GuideItem;
import com.hollispark.store.domain.Media;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ContentDtos {
    private ContentDtos() {}

    public record CollectionDto(
            @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "Use lowercase letters, digits and dashes.") @Size(max = 60) String id,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 300) String tagline,
            @Size(max = 2000) String intro,
            @Size(max = 600) String image,
            @Size(max = 500) String alt,
            @Size(max = 6) List<@Size(max = 300) String> choose,
            @Size(max = 6) List<GuideItem> guide,
            int sortOrder,
            boolean active) {

        public static CollectionDto of(CollectionEntity c) {
            return new CollectionDto(c.getSlug(), c.getName(), c.getTagline(), c.getIntro(), c.getImage(), c.getAlt(),
                    c.getChoose() == null ? List.of() : c.getChoose(), c.getGuide() == null ? List.of() : c.getGuide(),
                    c.getSortOrder(), c.isActive());
        }

        public void applyTo(CollectionEntity c) {
            c.setName(name.trim());
            c.setTagline(tagline);
            c.setIntro(intro);
            c.setImage(image);
            c.setAlt(alt);
            c.setChoose(choose == null ? List.of() : choose);
            c.setGuide(guide == null ? List.of() : guide);
            c.setSortOrder(sortOrder);
        }
    }

    /** Homepage content edited in the admin panel. */
    public record HomeContent(@NotNull @Valid Hero hero, @NotNull @Valid Banner banner, @Size(max = 80) String spotlightProductId) {
        public record Hero(@Size(max = 60) String eyebrow, @NotBlank @Size(max = 80) String title, @Size(max = 200) String subtitle,
                           @Size(max = 600) String image, @Size(max = 300) String imageAlt,
                           @Size(max = 40) String ctaLabel, @Size(max = 200) String ctaLink,
                           @Size(max = 40) String secondaryLabel, @Size(max = 200) String secondaryLink) {}

        public record Banner(boolean enabled, @Size(max = 140) String text, @Size(max = 200) String link) {}
    }

    public record MediaDto(UUID id, String url, String originalName, String contentType, long sizeBytes, String alt, Instant createdAt) {
        public static MediaDto of(Media m) {
            return new MediaDto(m.getId(), m.getUrl(), m.getOriginalName(), m.getContentType(), m.getSizeBytes(), m.getAlt(),
                    m.getCreatedAt());
        }
    }
}
