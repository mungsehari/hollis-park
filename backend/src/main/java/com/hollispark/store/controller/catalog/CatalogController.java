package com.hollispark.store.controller.catalog;

import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.catalog.CollectionDto;
import com.hollispark.store.dto.catalog.ProductDto;
import com.hollispark.store.dto.catalog.ReviewDto;
import com.hollispark.store.dto.content.PublicPick;
import com.hollispark.store.exception.ResourceNotFoundException;
import com.hollispark.store.service.catalog.StorefrontService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public catalog reads. All served from the in-memory storefront snapshot, never straight from the database. */
@Tag(name = "Catalog", description = "Public storefront data")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {
    private final StorefrontService storefront;

    @Operation(summary = "Everything the storefront needs to boot, in one request (promo codes are never included)")
    @GetMapping("/store")
    public ResponseEntity<ApiResponse<Map<String, Object>>> store() {
        return ResponseEntity.ok(ApiResponse.success(storefront.snapshot().bootstrap()));
    }

    @Operation(summary = "Live products, in display order")
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> products() {
        return ResponseEntity.ok(ApiResponse.success(storefront.snapshot().products()));
    }

    @Operation(summary = "One live product by its web address")
    @GetMapping("/products/{slug}")
    public ResponseEntity<ApiResponse<ProductDto>> product(@PathVariable String slug) {
        ProductDto p = storefront.snapshot().products().stream().filter(x -> x.id().equals(slug)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));
        return ResponseEntity.ok(ApiResponse.success(p));
    }

    @Operation(summary = "Reviews, optionally for one product")
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<ReviewDto>>> reviews(@RequestParam(required = false) String product) {
        var all = storefront.snapshot().reviews();
        return ResponseEntity.ok(ApiResponse.success(product == null ? all : all.stream().filter(r -> r.product().equals(product)).toList()));
    }

    @Operation(summary = "Live collections")
    @GetMapping("/collections")
    public ResponseEntity<ApiResponse<List<CollectionDto>>> collections() {
        return ResponseEntity.ok(ApiResponse.success(storefront.snapshot().collections()));
    }

    @Operation(summary = "Live care picks (Amazon / Flipkart affiliate products)")
    @GetMapping("/care-picks")
    public ResponseEntity<ApiResponse<List<PublicPick>>> carePicks() {
        return ResponseEntity.ok(ApiResponse.success(storefront.snapshot().picks()));
    }
}
