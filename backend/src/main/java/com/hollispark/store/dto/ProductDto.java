package com.hollispark.store.dto;

import com.hollispark.store.domain.FitNote;
import com.hollispark.store.domain.Product;
import com.hollispark.store.domain.WideOption;
import java.util.List;

/** Public product shape. Field names match what the storefront (and the seed files) use. */
public record ProductDto(
        String id, String name, String collection, String line, String colourway, String swatch,
        List<String> colours, int price, WideOption wide, double rating, int reviews, String flag,
        int rank, String added, String img, String context, String contextCaption, String alt,
        String blurb, String desc, List<List<String>> specs, List<List<String>> materials,
        FitNote fit, String care, List<Double> soldOut, List<Double> low, String keywords, List<String> gallery) {

    public static ProductDto of(Product p) {
        List<String> gallery = p.getGallery() == null || p.getGallery().isEmpty()
                ? (p.getImg() == null ? List.<String>of() : List.of(p.getImg())) : p.getGallery();
        return new ProductDto(p.getSlug(), p.getName(), p.getCollectionSlug(), p.getLineName(), p.getColourway(),
                p.getSwatch(), p.getColours(), p.getPrice(), p.getWide(), p.getRating(), p.getReviewCount(),
                p.getFlag(), p.getSortRank(), p.getAddedOn(), p.getImg(), p.getContextImg(), p.getContextCaption(),
                p.getAlt(), p.getBlurb(), p.getDescription(), p.getSpecs(), p.getMaterials(), p.getFit(), p.getCare(),
                p.getSoldOut(), p.getLow(), p.getKeywords(), gallery);
    }

    public Product toEntity() {
        List<String> photos = gallery == null ? null : gallery.stream().filter(u -> u != null && !u.isBlank()).toList();
        return Product.builder()
                .slug(id).name(name).collectionSlug(collection).lineName(line).colourway(colourway)
                .swatch(swatch).colours(colours).price(price).wide(wide).rating(rating)
                .reviewCount(reviews).flag(flag).sortRank(rank).addedOn(added)
                .img(photos != null && !photos.isEmpty() ? photos.get(0) : img)
                .contextImg(context).contextCaption(contextCaption).alt(alt).blurb(blurb).description(desc)
                .specs(specs).materials(materials).fit(fit).care(care)
                .soldOut(soldOut == null ? List.of() : soldOut).low(low == null ? List.of() : low)
                .keywords(keywords).gallery(photos)
                .build();
    }
}
