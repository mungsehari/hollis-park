package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "product_id")
    public String product;
    public String name;
    public String place;
    @Column(name = "shoe_size")
    public String size;
    @Column(name = "fit_score")
    public int fit;
    public int rating;
    @Column(name = "review_date")
    public String date;
    @Column(name = "body", length = 2000)
    public String text;
}
