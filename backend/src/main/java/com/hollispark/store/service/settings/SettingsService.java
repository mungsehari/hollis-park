package com.hollispark.store.service.settings;

import com.hollispark.store.entity.marketing.PromoCode;
import com.hollispark.store.entity.settings.StoreSettings;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.marketing.PromoCodeRepository;
import com.hollispark.store.repository.settings.StoreSettingsRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tax and delivery settings from the database, cached briefly so every request does not hit it. */
@Service
public class SettingsService {
    private static final Duration TTL = Duration.ofSeconds(30);

    public record Settings(String taxMode, BigDecimal taxRate, String taxName, int freeShipFrom, int shipFee, int expressFee) {
        public boolean taxIncluded() {
            return "included".equals(taxMode);
        }
    }

    private final StoreSettingsRepository repo;
    private final PromoCodeRepository promos;
    private volatile Settings cached;
    private volatile Instant loadedAt = Instant.EPOCH;

    public SettingsService(StoreSettingsRepository repo, PromoCodeRepository promos) {
        this.repo = repo;
        this.promos = promos;
    }

    public Settings get() {
        if (cached == null || Instant.now().isAfter(loadedAt.plus(TTL))) {
            StoreSettings s = repo.findFirstByOrderByCreatedAtAsc()
                    .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Store settings are missing."));
            cached = of(s);
            loadedAt = Instant.now();
        }
        return cached;
    }

    @Transactional
    public Settings update(Settings next) {
        StoreSettings s = repo.findFirstByOrderByCreatedAtAsc().orElseGet(() -> StoreSettings.builder().build());
        s.setTaxMode(next.taxMode());
        s.setTaxRate(next.taxRate());
        s.setTaxName(next.taxName().trim());
        s.setFreeShipFrom(next.freeShipFrom());
        s.setShipFee(next.shipFee());
        s.setExpressFee(next.expressFee());
        repo.save(s);
        cached = of(s);
        loadedAt = Instant.now();
        return cached;
    }

    /** Active promo code rate, or null. Codes are matched case-insensitively. */
    public BigDecimal promoRate(String code) {
        if (code == null || code.isBlank()) return null;
        return promos.findByCodeIgnoreCaseAndActiveTrue(code.trim()).map(PromoCode::getRate).orElse(null);
    }

    private static Settings of(StoreSettings s) {
        return new Settings(s.getTaxMode(), s.getTaxRate(), s.getTaxName(), s.getFreeShipFrom(), s.getShipFee(),
                s.getExpressFee());
    }
}
