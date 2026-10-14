package com.hollispark.store.entity.catalog;

import com.hollispark.store.entity.base.BaseEntity;
import com.hollispark.store.enums.Marketplace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Product extends BaseEntity {
    /** The product's web address, e.g. "tide-one". This is what the storefront and old orders refer to. */
    @Column(nullable = false, unique = true, updatable = false)
    private String slug;
    private String name;
    @Column(name = "collection_slug")
    private String collectionSlug;
    @Column(name = "line_name")
    private String lineName;
    private String colourway;
    private String swatch;
    /** Price in whole rupees for the standard width. */
    private int price;
    private double rating;
    @Column(name = "review_count")
    private int reviewCount;
    private String flag;
    @Column(name = "sort_rank")
    private int sortRank;
    @Column(name = "added_on")
    private String addedOn;
    private String img;
    @Column(name = "context_img")
    private String contextImg;
    private String contextCaption;
    private String alt;
    private String blurb;
    @Column(name = "description")
    private String description;
    private String care;
    private String keywords;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> colours;
    @JdbcTypeCode(SqlTypes.JSON)
    private WideOption wide;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<List<String>> specs;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<List<String>> materials;
    @JdbcTypeCode(SqlTypes.JSON)
    private FitNote fit;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Double> soldOut;
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Double> low;
    /** All photo URLs, main photo first. */
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> gallery;

    /** The same product on Amazon / Flipkart, optional ("Also on ..." on the product page). */
    @Column(name = "amazon_url")
    private String amazonUrl;
    @Column(name = "flipkart_url")
    private String flipkartUrl;

    /** Archived products disappear from the storefront but keep their order history. */
    @Builder.Default
    private boolean active = true;

    public boolean inStock(double size, boolean wideWidth) {
        if (soldOut != null && soldOut.contains(size)) return false;
        if (!wideWidth) return true;
        return wide != null && (wide.soldOut() == null || !wide.soldOut().contains(size));
    }

    /** The product's link on that marketplace, or null. */
    public String marketplaceUrl(Marketplace m) {
        String url = m == Marketplace.AMAZON ? amazonUrl : flipkartUrl;
        return url == null || url.isBlank() ? null : url;
    }

    public int unitPrice(boolean wideWidth) {
        return price + (wideWidth && wide != null ? wide.extra() : 0);
    }

    /** Copies every editable field from {@code o}; the id, slug, active flag and audit columns stay as they are. */
    public void copyFrom(Product o) {
        name = o.name; collectionSlug = o.collectionSlug; lineName = o.lineName; colourway = o.colourway;
        swatch = o.swatch; price = o.price; rating = o.rating; reviewCount = o.reviewCount; flag = o.flag;
        sortRank = o.sortRank; addedOn = o.addedOn; img = o.img; contextImg = o.contextImg;
        contextCaption = o.contextCaption; alt = o.alt; blurb = o.blurb; description = o.description;
        care = o.care; keywords = o.keywords; colours = o.colours; wide = o.wide; specs = o.specs;
        materials = o.materials; fit = o.fit; soldOut = o.soldOut; low = o.low; gallery = o.gallery;
        amazonUrl = o.amazonUrl; flipkartUrl = o.flipkartUrl;
    }
}
