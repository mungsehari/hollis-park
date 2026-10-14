package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.base.ActiveUpdate;
import com.hollispark.store.dto.catalog.CollectionDto;
import com.hollispark.store.service.catalog.CollectionService;
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

@Tag(name = "Admin: collections")
@RestController
@RequestMapping("/api/v1/admin/collections")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminCollectionController {
    private final CollectionService collections;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CollectionDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(collections.list()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CollectionDto>> get(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(collections.get(slug)));
    }

    @PostMapping
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<CollectionDto>> create(@Valid @RequestBody CollectionDto in) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(collections.create(in), HttpStatus.CREATED.value(), "Collection created"));
    }

    @PutMapping("/{slug}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<CollectionDto>> update(@PathVariable String slug, @Valid @RequestBody CollectionDto in) {
        return ResponseEntity.ok(ApiResponse.success(collections.update(slug, in), "Collection saved"));
    }

    @PatchMapping("/{slug}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<CollectionDto>> setActive(@PathVariable String slug, @Valid @RequestBody ActiveUpdate in) {
        return ResponseEntity.ok(ApiResponse.success(collections.setActive(slug, in.active())));
    }
}
