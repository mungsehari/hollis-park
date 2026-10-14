package com.hollispark.store.dto.catalog;

import com.hollispark.store.entity.catalog.Review;

public record ReviewDto(String product, String name, String place, String size, int fit, int rating,
                        String date, String text) {

    public static ReviewDto of(Review r) {
        return new ReviewDto(r.getProductSlug(), r.getName(), r.getPlace(), r.getSize(), r.getFit(), r.getRating(),
                r.getDate(), r.getText());
    }

    public Review toEntity() {
        return Review.builder().productSlug(product).name(name).place(place).size(size).fit(fit)
                .rating(rating).date(date).text(text).build();
    }
}
