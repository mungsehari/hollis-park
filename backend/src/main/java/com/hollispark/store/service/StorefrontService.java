package com.hollispark.store.service;

import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.CarePickRepository;
import com.hollispark.store.domain.CollectionRepository;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.ReviewRepository;
import com.hollispark.store.dto.ProductDto;
import com.hollispark.store.dto.ReviewDto;
import com.hollispark.store.dto.catalog.CollectionDto;
import com.hollispark.store.dto.content.PublicPick;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Everything the public storefront reads, built once and kept in memory. Visitors never wait for the
 * database; the snapshot is rebuilt after any admin change ({@link #changed()}) and at the latest after a minute.
 */
@Service
public class StorefrontService {
    public static final String CACHE = "storefront";

    /** One consistent view of the live catalog. {@code bootstrap} is the whole /api/store payload. */
    public record Snapshot(Map<String, Object> bootstrap, List<ProductDto> products, List<CollectionDto> collections,
                           List<ReviewDto> reviews, List<PublicPick> picks) {}

    private final StoreProperties props;
    private final StaticContent content;
    private final ProductRepository productRepo;
    private final ReviewRepository reviewRepo;
    private final CollectionRepository collectionRepo;
    private final SettingsService settings;
    private final HomeContentService home;
    private final CarePickRepository pickRepo;
    private final CacheManager cacheManager;

    public StorefrontService(StoreProperties props, StaticContent content, ProductRepository productRepo,
                             ReviewRepository reviewRepo, CollectionRepository collectionRepo,
                             SettingsService settings, HomeContentService home, CarePickRepository pickRepo,
                             CacheManager cacheManager) {
        this.cacheManager = cacheManager;
        this.pickRepo = pickRepo;
        this.props = props;
        this.content = content;
        this.productRepo = productRepo;
        this.reviewRepo = reviewRepo;
        this.collectionRepo = collectionRepo;
        this.settings = settings;
        this.home = home;
    }

    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        List<ProductDto> products = productRepo.findAllByActiveTrueOrderBySortRankAsc().stream().map(ProductDto::of).toList();
        Set<String> liveIds = products.stream().map(ProductDto::id).collect(Collectors.toSet());
        List<ReviewDto> reviews = reviewRepo.findAllByOrderBySortOrderAsc().stream()
                .filter(r -> liveIds.contains(r.getProductSlug())).map(ReviewDto::of).toList();
        List<CollectionDto> collections = collectionRepo.findAllByActiveTrueOrderBySortOrderAsc().stream()
                .map(CollectionDto::of).toList();
        List<PublicPick> picks = pickRepo.findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc().stream()
                .map(PublicPick::of).toList();

        Map<String, Object> site = new LinkedHashMap<>();
        site.put("name", props.name());
        site.put("store", props.storeName());
        site.put("orderPrefix", props.orderPrefix());

        SettingsService.Settings s = settings.get();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("locale", props.locale());
        config.put("currency", props.currency());
        config.put("taxMode", s.taxMode());
        config.put("taxRate", s.taxRate());
        config.put("taxName", s.taxName());
        config.put("freeShipFrom", s.freeShipFrom());
        config.put("shipFee", s.shipFee());
        config.put("expressFee", s.expressFee());
        config.put("sizeLabel", props.sizeLabel());
        config.put("postcode", props.postcode());

        // Promo codes are deliberately not part of this payload.
        Map<String, Object> bootstrap = new LinkedHashMap<>();
        bootstrap.put("site", site);
        bootstrap.put("config", config);
        bootstrap.put("payments", Map.of("enabled", props.razorpay().enabled(), "testMode", props.razorpay().testMode()));
        bootstrap.put("home", home.get());
        bootstrap.put("collections", collections);
        bootstrap.put("lines", content.lines);
        bootstrap.put("faq", content.faq);
        bootstrap.put("faqCats", content.faqCats);
        bootstrap.put("sizeChart", content.sizeChart);
        bootstrap.put("sizes", content.sizes);
        bootstrap.put("colours", content.colours);
        bootstrap.put("products", products);
        bootstrap.put("reviews", reviews);
        bootstrap.put("picks", picks);
        return new Snapshot(java.util.Collections.unmodifiableMap(bootstrap), products, collections, reviews, picks);
    }

    private void evict() {
        Cache cache = cacheManager.getCache(CACHE);
        if (cache != null) cache.clear();
    }

    /**
     * Call from an admin write. The snapshot is dropped after the transaction commits, so a visitor can
     * never re-fill it with the old data while the change is still being saved.
     */
    public void changed() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict();
                }
            });
        } else {
            evict();
        }
    }
}
