package com.hollispark.store.dto.catalog;

import com.hollispark.store.entity.catalog.CollectionEntity;
import com.hollispark.store.entity.catalog.GuideItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

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
