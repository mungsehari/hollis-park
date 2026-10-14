package com.hollispark.store.web;

import com.hollispark.store.dto.content.CarePickDto;
import com.hollispark.store.service.CarePickService;
import com.hollispark.store.service.StorefrontService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Care picks (affiliate products). Reads are open to staff; changes need ADMIN (SecurityConfig). */
@RestController
@RequestMapping("/api/admin/care-picks")
@RequiredArgsConstructor
public class AdminCarePicksController {
    private final CarePickService picks;
    private final StorefrontService storefront;

    @GetMapping
    public List<CarePickDto> list() {
        return picks.list().stream().map(CarePickDto::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CarePickDto create(@Valid @RequestBody CarePickDto in) {
        var saved = CarePickDto.of(picks.create(in));
        storefront.changed();
        return saved;
    }

    @PutMapping("/{id}")
    public CarePickDto update(@PathVariable UUID id, @Valid @RequestBody CarePickDto in) {
        var saved = CarePickDto.of(picks.update(id, in));
        storefront.changed();
        return saved;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        picks.delete(id);
        storefront.changed();
    }
}
