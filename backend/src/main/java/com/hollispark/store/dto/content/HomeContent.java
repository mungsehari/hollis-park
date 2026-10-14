package com.hollispark.store.dto.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Homepage content edited in the admin panel. */
public record HomeContent(@NotNull @Valid Hero hero, @NotNull @Valid Banner banner, @Size(max = 80) String spotlightProductId) {
    public record Hero(@Size(max = 60) String eyebrow, @NotBlank @Size(max = 80) String title, @Size(max = 200) String subtitle,
                       @Size(max = 600) String image, @Size(max = 300) String imageAlt,
                       @Size(max = 40) String ctaLabel, @Size(max = 200) String ctaLink,
                       @Size(max = 40) String secondaryLabel, @Size(max = 200) String secondaryLink) {}

    public record Banner(boolean enabled, @Size(max = 140) String text, @Size(max = 200) String link) {}
}
