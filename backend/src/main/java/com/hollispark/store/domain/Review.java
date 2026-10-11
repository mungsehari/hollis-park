package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Review extends BaseEntity {
    /** Slug of the reviewed product. */
    @Column(name = "product_slug", nullable = false)
    private String productSlug;
    private String name;
    private String place;
    @Column(name = "shoe_size")
    private String size;
    @Column(name = "fit_score")
    private int fit;
    private int rating;
    @Column(name = "review_date")
    private String date;
    @Column(name = "body", length = 2000)
    private String text;
    /** Position in the list of reviews. */
    @Column(name = "sort_order")
    private int sortOrder;
}
