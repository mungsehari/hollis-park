package com.hollispark.store.controller.admin;

import com.hollispark.store.config.Access;
import com.hollispark.store.dto.ApiResponse;
import com.hollispark.store.dto.marketing.PromoCreate;
import com.hollispark.store.dto.marketing.PromoDto;
import com.hollispark.store.dto.marketing.PromoUpdate;
import com.hollispark.store.service.marketing.PromoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin: promo codes")
@RestController
@RequestMapping("/api/v1/admin/promo-codes")
@RequiredArgsConstructor
@PreAuthorize(Access.ADMIN)
public class AdminPromoCodeController {
    private final PromoService promos;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(promos.list().stream().map(PromoDto::of).toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromoDto>> create(@Valid @RequestBody PromoCreate req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(PromoDto.of(promos.create(req.code(), req.rate())), HttpStatus.CREATED.value(), "Code created"));
    }

    @PatchMapping("/{code}")
    public ResponseEntity<ApiResponse<PromoDto>> update(@PathVariable String code, @Valid @RequestBody PromoUpdate req) {
        return ResponseEntity.ok(ApiResponse.success(PromoDto.of(promos.update(code, req.active(), req.rate()))));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String code) {
        promos.delete(code);
        return ResponseEntity.ok(ApiResponse.success(null, "Code deleted"));
    }
}
