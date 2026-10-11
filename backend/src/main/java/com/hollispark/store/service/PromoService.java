package com.hollispark.store.service;

import com.hollispark.store.domain.PromoCode;
import com.hollispark.store.domain.PromoCodeRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Discount codes. Codes are kept upper case and matched without regard to case. */
@Service
@RequiredArgsConstructor
public class PromoService {
    private final PromoCodeRepository promos;

    @Transactional(readOnly = true)
    public List<PromoCode> list() {
        return promos.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public PromoCode create(String code, BigDecimal rate) {
        String normalised = code.trim().toUpperCase();
        if (promos.existsByCodeIgnoreCase(normalised)) throw new ApiException(HttpStatus.CONFLICT, "This code already exists.");
        return promos.save(PromoCode.builder().code(normalised).rate(rate).build());
    }

    @Transactional
    public PromoCode update(String code, Boolean active, BigDecimal rate) {
        PromoCode p = find(code);
        if (active != null) p.setActive(active);
        if (rate != null) p.setRate(rate);
        return p;
    }

    @Transactional
    public void delete(String code) {
        promos.delete(find(code));
    }

    private PromoCode find(String code) {
        return promos.findByCodeIgnoreCase(code.trim()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Code not found."));
    }
}
