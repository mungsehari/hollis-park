package com.hollispark.store.dto.content;

import com.hollispark.store.domain.CarePick;
import java.util.UUID;

/** Storefront shape: no raw link, the store sends people through the tracked redirect. */
public record PublicPick(UUID id, String title, String blurb, String image, String marketplace, String marketplaceLabel) {
    public static PublicPick of(CarePick p) {
        return new PublicPick(p.getId(), p.getTitle(), p.getBlurb(), p.getImage(), p.getMarketplace().name(),
                p.getMarketplace().label());
    }
}
