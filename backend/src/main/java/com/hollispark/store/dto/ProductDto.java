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
        return new ProductDto(p.id, p.name, p.collection, p.line, p.colourway, p.swatch, p.colours, p.price,
                p.wide, p.rating, p.reviewCount, p.flag, p.rank, p.added, p.img, p.context, p.contextCaption,
                p.alt, p.blurb, p.desc, p.specs, p.materials, p.fit, p.care, p.soldOut, p.low, p.keywords,
                p.gallery == null || p.gallery.isEmpty() ? (p.img == null ? List.of() : List.of(p.img)) : p.gallery);
    }

    public Product toEntity() {
        Product p = new Product();
        p.id = id; p.name = name; p.collection = collection; p.line = line; p.colourway = colourway;
        p.swatch = swatch; p.colours = colours; p.price = price; p.wide = wide; p.rating = rating;
        p.reviewCount = reviews; p.flag = flag; p.rank = rank; p.added = added; p.img = img;
        p.context = context; p.contextCaption = contextCaption; p.alt = alt; p.blurb = blurb; p.desc = desc;
        p.specs = specs; p.materials = materials; p.fit = fit; p.care = care;
        p.soldOut = soldOut == null ? List.of() : soldOut; p.low = low == null ? List.of() : low;
        p.keywords = keywords;
        p.gallery = gallery == null ? null : gallery.stream().filter(u -> u != null && !u.isBlank()).toList();
        if (p.gallery != null && !p.gallery.isEmpty()) p.img = p.gallery.get(0);
        return p;
    }
}
