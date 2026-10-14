package com.hollispark.store.service.catalog;

import com.hollispark.store.dto.catalog.ProductAdmin;
import com.hollispark.store.dto.catalog.ProductDto;
import com.hollispark.store.entity.catalog.Product;
import com.hollispark.store.enums.Marketplace;
import com.hollispark.store.exception.BadRequestException;
import com.hollispark.store.exception.ConflictException;
import com.hollispark.store.exception.ResourceNotFoundException;
import com.hollispark.store.repository.catalog.CollectionRepository;
import com.hollispark.store.repository.catalog.ProductRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Product management for the admin panel. Archiving (active = false) hides a product without breaking old orders. */
@Service
@RequiredArgsConstructor
public class ProductAdminService {
    private static final String SLUG = "[a-z0-9]+(-[a-z0-9]+)*";

    private final ProductRepository products;
    private final CollectionRepository collections;
    private final StaticContent content;
    private final StorefrontService storefront;

    @Transactional(readOnly = true)
    public List<ProductAdmin> list() {
        return products.findAllByOrderBySortRankAsc().stream().map(ProductAdminService::toAdmin).toList();
    }

    @Transactional(readOnly = true)
    public ProductAdmin get(String slug) {
        return toAdmin(find(slug));
    }

    @Transactional
    public ProductAdmin create(ProductDto in) {
        if (in.id() == null || !in.id().matches(SLUG) || in.id().length() > 80) {
            throw new BadRequestException("Use a short web address like 'tide-two' (lowercase letters, digits and dashes).");
        }
        if (products.existsBySlug(in.id())) throw new ConflictException("A product with this web address already exists.");
        validate(in);
        Product saved = products.saveAndFlush(in.toEntity());
        storefront.changed();
        return toAdmin(saved);
    }

    @Transactional
    public ProductAdmin update(String slug, ProductDto in) {
        Product existing = find(slug);
        validate(in);
        existing.copyFrom(in.toEntity());
        Product saved = products.saveAndFlush(existing);
        storefront.changed();
        return toAdmin(saved);
    }

    @Transactional
    public ProductAdmin setActive(String slug, boolean active) {
        Product p = find(slug);
        p.setActive(active);
        products.flush();
        storefront.changed();
        return toAdmin(p);
    }

    /** All the rules at once, so the admin sees every problem in one message instead of one per save. */
    private void validate(ProductDto in) {
        List<String> problems = new ArrayList<>();
        if (in.name() == null || in.name().isBlank()) problems.add("Add a name.");
        if (in.collection() == null || !collections.existsBySlug(in.collection())) problems.add("Choose one of the collections.");
        if (in.price() <= 0) problems.add("The price must be more than zero.");
        if (in.wide() != null && in.wide().extra() < 0) problems.add("The Wide supplement cannot be negative.");
        if (in.rating() < 0 || in.rating() > 5) problems.add("Rating must be between 0 and 5.");
        Set<Double> sizes = new HashSet<>();
        content.sizes.forEach(n -> sizes.add(n.asDouble()));
        for (List<Double> list : Arrays.asList(in.soldOut(), in.low(), in.wide() == null ? null : in.wide().soldOut())) {
            if (list != null && !sizes.containsAll(list)) problems.add("Sold-out and low-stock sizes must be real sizes.");
        }
        boolean hasPhoto = (in.gallery() != null && in.gallery().stream().anyMatch(u -> u != null && !u.isBlank()))
                || (in.img() != null && !in.img().isBlank());
        if (!hasPhoto) problems.add("Add at least one photo.");
        if (in.fit() == null || in.fit().verdict() == null || in.fit().verdict().isBlank()) problems.add("Add a fit verdict.");
        if (in.amazonUrl() != null && !in.amazonUrl().isBlank() && !Marketplace.AMAZON.accepts(in.amazonUrl())) {
            problems.add("The Amazon link must be an https link on amazon.in or amzn.to.");
        }
        if (in.flipkartUrl() != null && !in.flipkartUrl().isBlank() && !Marketplace.FLIPKART.accepts(in.flipkartUrl())) {
            problems.add("The Flipkart link must be an https link on flipkart.com.");
        }
        if (!problems.isEmpty()) throw new BadRequestException(String.join(" ", problems.stream().distinct().toList()));
    }

    private Product find(String slug) {
        return products.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Product not found."));
    }

    private static ProductAdmin toAdmin(Product p) {
        return new ProductAdmin(ProductDto.of(p), p.isActive(), p.getUpdatedAt());
    }
}
