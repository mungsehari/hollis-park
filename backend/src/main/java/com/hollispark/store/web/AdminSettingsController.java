package com.hollispark.store.web;

import com.hollispark.store.dto.marketing.PromoCreate;
import com.hollispark.store.dto.marketing.PromoDto;
import com.hollispark.store.dto.marketing.PromoUpdate;
import com.hollispark.store.dto.settings.SettingsDto;
import com.hollispark.store.service.PromoService;
import com.hollispark.store.service.SettingsService;
import com.hollispark.store.service.StorefrontService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tax and delivery settings, and promo codes. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminSettingsController {
    private final SettingsService settings;
    private final PromoService promos;
    private final StorefrontService storefront;

    @GetMapping("/settings")
    public SettingsService.Settings settings() {
        return settings.get();
    }

    @PutMapping("/settings")
    public SettingsService.Settings updateSettings(@Valid @RequestBody SettingsDto req) {
        var updated = settings.update(new SettingsService.Settings(req.taxMode(), req.taxRate(), req.taxName(),
                req.freeShipFrom(), req.shipFee(), req.expressFee()));
        storefront.changed();
        return updated;
    }

    @GetMapping("/promo-codes")
    public List<PromoDto> promoCodes() {
        return promos.list().stream().map(PromoDto::of).toList();
    }

    @PostMapping("/promo-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public PromoDto createPromo(@Valid @RequestBody PromoCreate req) {
        return PromoDto.of(promos.create(req.code(), req.rate()));
    }

    @PatchMapping("/promo-codes/{code}")
    public PromoDto updatePromo(@PathVariable String code, @Valid @RequestBody PromoUpdate req) {
        return PromoDto.of(promos.update(code, req.active(), req.rate()));
    }

    @DeleteMapping("/promo-codes/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePromo(@PathVariable String code) {
        promos.delete(code);
    }
}
