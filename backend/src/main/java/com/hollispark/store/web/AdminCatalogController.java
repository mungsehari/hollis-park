package com.hollispark.store.web;

import com.hollispark.store.domain.CollectionRepository;
import com.hollispark.store.domain.Marketplace;
import com.hollispark.store.domain.Product;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.dto.ProductDto;
import com.hollispark.store.dto.base.ActiveUpdate;
import com.hollispark.store.dto.catalog.ProductAdmin;
import com.hollispark.store.service.ApiException;
import com.hollispark.store.service.StaticContent;
import com.hollispark.store.service.StorefrontService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Product management. Archiving (active = false) hides a product from the store without breaking old orders. */
@RestController
@RequestMapping("/api/admin/products")
public class AdminCatalogController {
    private final ProductRepository products;
    private final StaticContent content;
    private final CollectionRepository collections;
    private final StorefrontService storefront;

    public AdminCatalogController(ProductRepository products, StaticContent content, CollectionRepository collections,
                                  StorefrontService storefront) {
        this.products = products;
        this.content = content;
        this.collections = collections;
        this.storefront = storefront;
    }

    @GetMapping
    public List<ProductAdmin> list() {
        return products.findAllByOrderBySortRankAsc().stream().map(AdminCatalogController::admin).toList();
    }

    @GetMapping("/{id}")
    public ProductAdmin get(@PathVariable String id) {
        return admin(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductAdmin create(@Valid @RequestBody ProductDto in) {
        if (in.id() == null || !in.id().matches("[a-z0-9]+(-[a-z0-9]+)*") || in.id().length() > 80) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Use a short web address like 'tide-two' (lowercase letters, digits and dashes).");
        }
        if (products.existsBySlug(in.id())) throw new ApiException(HttpStatus.CONFLICT, "A product with this web address already exists.");
        validate(in);
        Product p = in.toEntity();
        var saved = products.saveAndFlush(p);
        storefront.changed();
        return admin(saved);
    }

    @PutMapping("/{id}")
    @Transactional
    public ProductAdmin update(@PathVariable String id, @Valid @RequestBody ProductDto in) {
        Product existing = find(id);
        validate(in);
        existing.copyFrom(in.toEntity());
        var saved = products.saveAndFlush(existing);
        storefront.changed();
        return admin(saved);
    }

    @PatchMapping("/{id}")
    @Transactional
    public ProductAdmin setActive(@PathVariable String id, @Valid @RequestBody ActiveUpdate in) {
        Product p = find(id);
        p.setActive(in.active());
        products.flush();
        storefront.changed();
        return admin(p);
    }

    private void validate(ProductDto in) {
        List<String> problems = new ArrayList<>();
        if (in.name() == null || in.name().isBlank()) problems.add("Add a name.");
        if (in.collection() == null || !collections.existsBySlug(in.collection())) problems.add("Choose one of the collections.");
        if (in.price() <= 0) problems.add("The price must be more than zero.");
        if (in.wide() != null && in.wide().extra() < 0) problems.add("The Wide supplement cannot be negative.");
        if (in.rating() < 0 || in.rating() > 5) problems.add("Rating must be between 0 and 5.");
        Set<Double> sizes = new HashSet<>();
        content.sizes.forEach(n -> sizes.add(n.asDouble()));
        for (List<Double> list : new ArrayList<>(java.util.Arrays.asList(in.soldOut(), in.low(), in.wide() == null ? null : in.wide().soldOut()))) {
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
        if (!problems.isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, String.join(" ", problems.stream().distinct().toList()));
    }

    private Product find(String id) {
        return products.findBySlug(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    private static ProductAdmin admin(Product p) {
        return new ProductAdmin(ProductDto.of(p), p.isActive(), p.getUpdatedAt());
    }
}
