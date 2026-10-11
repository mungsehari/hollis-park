package com.hollispark.store.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.CollectionEntity;
import com.hollispark.store.domain.CollectionRepository;
import com.hollispark.store.domain.GuideItem;
import com.hollispark.store.domain.Product;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.PromoCode;
import com.hollispark.store.domain.PromoCodeRepository;
import com.hollispark.store.domain.Review;
import com.hollispark.store.domain.ReviewRepository;
import com.hollispark.store.domain.StaffUser;
import com.hollispark.store.domain.StaffUserRepository;
import com.hollispark.store.domain.StoreSettings;
import com.hollispark.store.domain.StoreSettingsRepository;
import com.hollispark.store.dto.ProductDto;
import com.hollispark.store.dto.ReviewDto;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * First-start data: the placeholder catalog, default tax/delivery settings and promo codes,
 * and the first admin account. Each step only runs when its table is empty.
 */
@Component
public class CatalogSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private final ProductRepository products;
    private final ReviewRepository reviews;
    private final StoreSettingsRepository settings;
    private final PromoCodeRepository promos;
    private final StaffUserRepository staff;
    private final PasswordEncoder encoder;
    private final StoreProperties props;
    private final ObjectMapper mapper;
    private final MediaService media;
    private final CollectionRepository collections;
    private final HomeContentService home;

    public CatalogSeeder(ProductRepository products, ReviewRepository reviews, StoreSettingsRepository settings,
                         PromoCodeRepository promos, StaffUserRepository staff, PasswordEncoder encoder,
                         StoreProperties props, ObjectMapper mapper, MediaService media,
                         CollectionRepository collections, HomeContentService home) {
        this.products = products;
        this.reviews = reviews;
        this.settings = settings;
        this.promos = promos;
        this.staff = staff;
        this.encoder = encoder;
        this.props = props;
        this.mapper = mapper;
        this.media = media;
        this.collections = collections;
        this.home = home;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        // Order matters: products point at collections, reviews point at products.
        Map<String, String> images = seedImages();
        if (collections.count() == 0) {
            seedCollections(images);
            collections.flush(); // the database checks the product -> collection link on insert
        }
        if (products.count() == 0) {
            List<ProductDto> list = read("seed/products.json", new TypeReference<>() {});
            products.saveAllAndFlush(list.stream().map(ProductDto::toEntity).toList());
            log.info("Seeded {} products", list.size());
        }
        if (reviews.count() == 0) {
            List<ReviewDto> list = read("seed/reviews.json", new TypeReference<>() {});
            List<Review> entities = new ArrayList<>();
            for (ReviewDto dto : list) {
                Review r = dto.toEntity();
                r.setSortOrder(entities.size());
                entities.add(r);
            }
            reviews.saveAll(entities);
            log.info("Seeded {} reviews", list.size());
        }
        moveProductImagesToMedia(images);
        var d = props.defaults();
        if (settings.count() == 0) {
            settings.save(StoreSettings.builder()
                    .taxMode(d.taxMode()).taxRate(d.taxRate()).taxName(d.taxName())
                    .freeShipFrom(d.freeShipFrom()).shipFee(d.shipFee()).expressFee(d.expressFee())
                    .build());
        }
        if (promos.count() == 0 && d.promoCodes() != null) {
            d.promoCodes().forEach((code, rate) ->
                    promos.save(PromoCode.builder().code(code.toUpperCase()).rate(rate).build()));
        }
        if (!home.exists()) home.save(HomeContentService.defaults(images.get("hero-portrait")));
        bootstrapAdmin();
    }

    /** Placeholder photos ship in the jar; they are copied into media storage so every image is replaceable. */
    private Map<String, String> seedImages() throws IOException {
        Map<String, String> urls = new HashMap<>();
        for (Resource r : new PathMatchingResourcePatternResolver().getResources("classpath:seed/images/*.webp")) {
            String file = r.getFilename();
            if (file == null) continue;
            try (InputStream in = r.getInputStream()) {
                urls.put(file.replace(".webp", ""), media.seed(file, in.readAllBytes()));
            }
        }
        return urls;
    }

    /** Older rows hold image names ("p-tide-one"); turn them into media URLs and a gallery. */
    private void moveProductImagesToMedia(Map<String, String> images) {
        for (Product p : products.findAll()) {
            boolean changed = false;
            if (p.getImg() != null && images.containsKey(p.getImg())) { p.setImg(images.get(p.getImg())); changed = true; }
            if (p.getContextImg() != null && images.containsKey(p.getContextImg())) {
                p.setContextImg(images.get(p.getContextImg()));
                changed = true;
            }
            if (p.getGallery() == null || p.getGallery().isEmpty()) {
                List<String> g = new ArrayList<>();
                if (p.getImg() != null) g.add(p.getImg());
                if (p.getContextImg() != null) g.add(p.getContextImg());
                p.setGallery(g);
                changed = true;
            }
            if (changed) products.save(p);
        }
    }

    private void seedCollections(Map<String, String> images) throws IOException {
        JsonNode list = read("seed/collections.json", new TypeReference<JsonNode>() {});
        int order = 0;
        for (JsonNode n : list) {
            collections.save(CollectionEntity.builder()
                    .slug(n.get("id").asText())
                    .name(n.get("name").asText())
                    .tagline(n.path("tagline").asText(null))
                    .intro(n.path("intro").asText(null))
                    .image(images.getOrDefault(n.path("img").asText(), null))
                    .alt(n.path("alt").asText(null))
                    .choose(mapper.convertValue(n.path("choose"), new TypeReference<List<String>>() {}))
                    .guide(mapper.convertValue(n.path("guide"), new TypeReference<List<GuideItem>>() {}))
                    .sortOrder(order++)
                    .build());
        }
        log.info("Seeded {} collections", list.size());
    }

    private void bootstrapAdmin() {
        if (staff.count() > 0) return;
        var b = props.bootstrapAdmin();
        if (b == null || b.email() == null || b.email().isBlank() || b.password() == null || b.password().isBlank()) {
            log.warn("No staff users and no ADMIN_EMAIL / ADMIN_PASSWORD set: nobody can sign in to the admin panel yet.");
            return;
        }
        if (b.password().length() < 10) {
            log.warn("ADMIN_PASSWORD must be at least 10 characters; the first admin was not created.");
            return;
        }
        StaffUser u = staff.save(StaffUser.builder()
                .email(b.email().trim().toLowerCase())
                .name(b.name() == null || b.name().isBlank() ? "Store owner" : b.name())
                .passwordHash(encoder.encode(b.password()))
                .role(StaffUser.Role.ADMIN)
                .build());
        log.info("Created the first admin account {}", u.getEmail());
    }

    private <T> T read(String path, TypeReference<T> type) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        }
    }
}
