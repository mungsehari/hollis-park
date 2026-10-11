package com.hollispark.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {
    @Id
    public String id;
    public String name;
    @Column(name = "collection_id")
    public String collection;
    @Column(name = "line_name")
    public String line;
    public String colourway;
    public String swatch;
    /** Price in whole rupees for the standard width. */
    public int price;
    public double rating;
    @Column(name = "review_count")
    public int reviewCount;
    public String flag;
    @Column(name = "sort_rank")
    public int rank;
    @Column(name = "added_on")
    public String added;
    public String img;
    @Column(name = "context_img")
    public String context;
    public String contextCaption;
    public String alt;
    public String blurb;
    @Column(name = "description")
    public String desc;
    public String care;
    public String keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> colours;
    @JdbcTypeCode(SqlTypes.JSON)
    public WideOption wide;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<List<String>> specs;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<List<String>> materials;
    @JdbcTypeCode(SqlTypes.JSON)
    public FitNote fit;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<Double> soldOut;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<Double> low;
    /** All photo URLs, main photo first. */
    @JdbcTypeCode(SqlTypes.JSON)
    public List<String> gallery;

    /** Archived products disappear from the storefront but keep their order history. */
    public boolean active = true;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public boolean inStock(double size, boolean wideWidth) {
        if (soldOut != null && soldOut.contains(size)) return false;
        if (!wideWidth) return true;
        return wide != null && (wide.soldOut() == null || !wide.soldOut().contains(size));
    }

    public int unitPrice(boolean wideWidth) {
        return price + (wideWidth && wide != null ? wide.extra() : 0);
    }
}
