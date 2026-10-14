package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.content.HomeContent;
import com.hollispark.store.service.catalog.StorefrontService;
import com.hollispark.store.service.content.HomeContentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: homepage")
@RestController
@RequestMapping("/api/v1/admin/home")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminHomeController {
    private final HomeContentService home;
    private final StorefrontService storefront;

    @GetMapping
    public ResponseEntity<ApiResponse<HomeContent>> get() {
        return ResponseEntity.ok(ApiResponse.success(home.get()));
    }

    @PutMapping
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<HomeContent>> save(@Valid @RequestBody HomeContent in) {
        HomeContent saved = home.save(in);
        storefront.changed();
        return ResponseEntity.ok(ApiResponse.success(saved, "Homepage saved"));
    }
}
