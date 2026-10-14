package com.hollispark.store.web;

import com.hollispark.store.dto.ProductDto;
import com.hollispark.store.dto.ReviewDto;
import com.hollispark.store.dto.catalog.CollectionDto;
import com.hollispark.store.service.ApiException;
import com.hollispark.store.service.StorefrontService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final StorefrontService storefront;

    public CatalogController(StorefrontService storefront) {
        this.storefront = storefront;
    }

    /** Everything the storefront needs to boot, in one request. Promo codes are deliberately not included. */
    @GetMapping("/store")
    public Map<String, Object> store() {
        return storefront.snapshot().bootstrap();
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return storefront.snapshot().products();
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable String id) {
        return storefront.snapshot().products().stream().filter(p -> p.id().equals(id)).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    @GetMapping("/reviews")
    public List<ReviewDto> reviews(@RequestParam(required = false) String product) {
        var all = storefront.snapshot().reviews();
        return product == null ? all : all.stream().filter(r -> r.product().equals(product)).toList();
    }

    @GetMapping("/collections")
    public List<CollectionDto> collections() {
        return storefront.snapshot().collections();
    }
}
