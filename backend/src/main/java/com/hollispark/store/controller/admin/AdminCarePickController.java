package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.content.CarePickDto;
import com.hollispark.store.service.catalog.StorefrontService;
import com.hollispark.store.service.content.CarePickService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: care picks")
@RestController
@RequestMapping("/api/v1/admin/care-picks")
@RequiredArgsConstructor
@PreAuthorize(Access.STAFF)
public class AdminCarePickController {
    private final CarePickService picks;
    private final StorefrontService storefront;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarePickDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(picks.list().stream().map(CarePickDto::of).toList()));
    }

    @PostMapping
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<CarePickDto>> create(@Valid @RequestBody CarePickDto in) {
        CarePickDto saved = CarePickDto.of(picks.create(in));
        storefront.changed();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(saved, HttpStatus.CREATED.value(), "Pick added"));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<CarePickDto>> update(@PathVariable UUID id, @Valid @RequestBody CarePickDto in) {
        CarePickDto saved = CarePickDto.of(picks.update(id, in));
        storefront.changed();
        return ResponseEntity.ok(ApiResponse.success(saved, "Pick saved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Access.ADMIN)
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        picks.delete(id);
        storefront.changed();
        return ResponseEntity.ok(ApiResponse.success(null, "Pick removed"));
    }
}
