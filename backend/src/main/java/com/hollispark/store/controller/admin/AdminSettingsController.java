package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.settings.SettingsDto;
import com.hollispark.store.service.catalog.StorefrontService;
import com.hollispark.store.service.settings.SettingsService;
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

@Tag(name = "Admin: tax and delivery")
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminSettingsController {
    private final SettingsService settings;
    private final StorefrontService storefront;

    @GetMapping
    public ResponseEntity<ApiResponse<SettingsService.Settings>> get() {
        return ResponseEntity.ok(ApiResponse.success(settings.get()));
    }

    @PutMapping
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<SettingsService.Settings>> update(@Valid @RequestBody SettingsDto req) {
        var updated = settings.update(new SettingsService.Settings(req.taxMode(), req.taxRate(), req.taxName(),
                req.freeShipFrom(), req.shipFee(), req.expressFee()));
        storefront.changed();
        return ResponseEntity.ok(ApiResponse.success(updated, "Settings saved"));
    }
}
