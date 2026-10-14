package com.hollispark.store.dto.content;

import com.hollispark.store.entity.content.CarePick;
import com.hollispark.store.enums.Marketplace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Admin shape: everything, including the affiliate link. */
public record CarePickDto(
        UUID id,
        @NotBlank @Size(max = 120) String title,
        @Size(max = 300) String blurb,
        @Size(max = 600) String image,
        @NotNull Marketplace marketplace,
        @NotBlank @Size(max = 600) String url,
        int sortOrder,
        boolean active) {

    public static CarePickDto of(CarePick p) {
        return new CarePickDto(p.getId(), p.getTitle(), p.getBlurb(), p.getImage(), p.getMarketplace(), p.getUrl(),
                p.getSortOrder(), p.isActive());
    }

    public void applyTo(CarePick p) {
        p.setTitle(title.trim());
        p.setBlurb(blurb == null || blurb.isBlank() ? null : blurb.trim());
        p.setImage(image == null || image.isBlank() ? null : image.trim());
        p.setMarketplace(marketplace);
        p.setUrl(url.trim());
        p.setSortOrder(sortOrder);
        p.setActive(active);
    }
}
