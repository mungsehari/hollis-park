package com.hollispark.store.dto;

import com.hollispark.store.domain.Review;

public record ReviewDto(String product, String name, String place, String size, int fit, int rating,
                        String date, String text) {

    public static ReviewDto of(Review r) {
        return new ReviewDto(r.product, r.name, r.place, r.size, r.fit, r.rating, r.date, r.text);
    }

    public Review toEntity() {
        Review r = new Review();
        r.product = product; r.name = name; r.place = place; r.size = size; r.fit = fit;
        r.rating = rating; r.date = date; r.text = text;
        return r;
    }
}
