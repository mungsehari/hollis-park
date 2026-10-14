package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.base.ActiveUpdate;
import com.hollispark.store.dto.catalog.ProductAdmin;
import com.hollispark.store.dto.catalog.ProductDto;
import com.hollispark.store.service.catalog.ProductAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: products")
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminProductController {
    private final ProductAdminService products;

    @Operation(summary = "All products, archived included")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductAdmin>>> list() {
        return ResponseEntity.ok(ApiResponse.success(products.list()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ProductAdmin>> get(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(products.get(slug)));
    }

    @Operation(summary = "Create a product")
    @PostMapping
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<ProductAdmin>> create(@Valid @RequestBody ProductDto in) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(products.create(in), HttpStatus.CREATED.value(), "Product created"));
    }

    @Operation(summary = "Replace a product's details (the web address cannot change)")
    @PutMapping("/{slug}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<ProductAdmin>> update(@PathVariable String slug, @Valid @RequestBody ProductDto in) {
        return ResponseEntity.ok(ApiResponse.success(products.update(slug, in), "Product saved"));
    }

    @Operation(summary = "Archive or restore")
    @PatchMapping("/{slug}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<ProductAdmin>> setActive(@PathVariable String slug, @Valid @RequestBody ActiveUpdate in) {
        return ResponseEntity.ok(ApiResponse.success(products.setActive(slug, in.active())));
    }
}
