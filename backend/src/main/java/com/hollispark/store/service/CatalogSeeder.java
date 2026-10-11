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
import com.hollispark.store.domain.ReviewRepository;
import com.hollispark.store.domain.StaffUser;
import com.hollispark.store.domain.StaffUserRepository;
import com.hollispark.store.domain.StoreSettingsRepository;
import com.hollispark.store.dto.ProductDto;
import com.hollispark.store.dto.ReviewDto;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
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
        if (products.count() == 0) {
            List<ProductDto> list = read("seed/products.json", new TypeReference<>() {});
            products.saveAll(list.stream().map(ProductDto::toEntity).toList());
            log.info("Seeded {} products", list.size());
        }
        if (reviews.count() == 0) {
            List<ReviewDto> list = read("seed/reviews.json", new TypeReference<>() {});
            reviews.saveAll(list.stream().map(ReviewDto::toEntity).toList());
            log.info("Seeded {} reviews", list.size());
        }
        var d = props.defaults();
        if (settings.count() == 0) {
            var s = new com.hollispark.store.domain.StoreSettings();
            s.taxMode = d.taxMode(); s.taxRate = d.taxRate(); s.taxName = d.taxName();
            s.freeShipFrom = d.freeShipFrom(); s.shipFee = d.shipFee(); s.expressFee = d.expressFee();
            s.updatedAt = Instant.now();
            settings.save(s);
        }
        if (promos.count() == 0 && d.promoCodes() != null) {
            d.promoCodes().forEach((code, rate) -> {
                PromoCode p = new PromoCode();
                p.code = code.toUpperCase(); p.rate = rate; p.active = true; p.createdAt = Instant.now();
                promos.save(p);
            });
        }
        Map<String, String> images = seedImages();
        moveProductImagesToMedia(images);
        if (collections.count() == 0) seedCollections(images);
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
            if (p.img != null && images.containsKey(p.img)) { p.img = images.get(p.img); changed = true; }
            if (p.context != null && images.containsKey(p.context)) { p.context = images.get(p.context); changed = true; }
            if (p.gallery == null || p.gallery.isEmpty()) {
                List<String> g = new ArrayList<>();
                if (p.img != null) g.add(p.img);
                if (p.context != null) g.add(p.context);
                p.gallery = g;
                changed = true;
            }
            if (changed) products.save(p);
        }
    }

    private void seedCollections(Map<String, String> images) throws IOException {
        JsonNode list = read("seed/collections.json", new TypeReference<JsonNode>() {});
        int order = 0;
        for (JsonNode n : list) {
            CollectionEntity c = new CollectionEntity();
            c.id = n.get("id").asText();
            c.name = n.get("name").asText();
            c.tagline = n.path("tagline").asText(null);
            c.intro = n.path("intro").asText(null);
            c.image = images.getOrDefault(n.path("img").asText(), null);
            c.alt = n.path("alt").asText(null);
            c.choose = mapper.convertValue(n.path("choose"), new TypeReference<List<String>>() {});
            c.guide = mapper.convertValue(n.path("guide"), new TypeReference<List<GuideItem>>() {});
            c.sortOrder = order++;
            c.active = true;
            c.updatedAt = Instant.now();
            collections.save(c);
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
        StaffUser u = new StaffUser();
        u.email = b.email().trim().toLowerCase();
        u.name = b.name() == null || b.name().isBlank() ? "Store owner" : b.name();
        u.passwordHash = encoder.encode(b.password());
        u.role = StaffUser.Role.ADMIN;
        u.active = true;
        u.createdAt = Instant.now();
        staff.save(u);
        log.info("Created the first admin account {}", u.email);
    }

    private <T> T read(String path, TypeReference<T> type) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        }
    }
}
