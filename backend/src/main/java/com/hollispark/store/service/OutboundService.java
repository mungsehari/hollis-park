package com.hollispark.store.service;

import com.hollispark.store.domain.CarePick;
import com.hollispark.store.domain.CarePickRepository;
import com.hollispark.store.domain.Marketplace;
import com.hollispark.store.domain.OutboundClick;
import com.hollispark.store.domain.OutboundClickRepository;
import com.hollispark.store.domain.Product;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.enums.OutboundTargetType;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a click on "Buy on Amazon / Flipkart" into the marketplace link, counting it on the way. */
@Service
@RequiredArgsConstructor
public class OutboundService {
    private final ProductRepository products;
    private final CarePickRepository picks;
    private final OutboundClickRepository clicks;

    /** The product's link on that marketplace, or empty when the product or link does not exist. */
    @Transactional
    public Optional<String> productLink(String slug, Marketplace marketplace) {
        Optional<Product> product = products.findBySlugAndActiveTrue(slug);
        String url = product.map(p -> p.marketplaceUrl(marketplace)).orElse(null);
        if (url == null || !marketplace.accepts(url)) return Optional.empty();
        record(OutboundTargetType.PRODUCT, slug, product.get().getName(), marketplace);
        return Optional.of(url);
    }

    @Transactional
    public Optional<String> pickLink(UUID id) {
        Optional<CarePick> pick = picks.findByIdAndActiveTrue(id);
        if (pick.isEmpty() || !pick.get().getMarketplace().accepts(pick.get().getUrl())) return Optional.empty();
        CarePick p = pick.get();
        record(OutboundTargetType.PICK, id.toString(), p.getTitle(), p.getMarketplace());
        return Optional.of(p.getUrl());
    }

    private void record(OutboundTargetType type, String key, String name, Marketplace marketplace) {
        clicks.save(OutboundClick.builder().targetType(type).targetKey(key).targetName(name).marketplace(marketplace).build());
    }
}
