package com.hollispark.store.web;

import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.ReviewRepository;
import com.hollispark.store.dto.ProductDto;
import com.hollispark.store.dto.ReviewDto;
import com.hollispark.store.service.ApiException;
import com.hollispark.store.domain.CollectionRepository;
import com.hollispark.store.dto.ContentDtos.CollectionDto;
import com.hollispark.store.service.HomeContentService;
import com.hollispark.store.service.SettingsService;
import com.hollispark.store.service.StaticContent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final StoreProperties props;
    private final StaticContent content;
    private final ProductRepository products;
    private final ReviewRepository reviews;
    private final SettingsService settings;
    private final CollectionRepository collections;
    private final HomeContentService home;

    public CatalogController(StoreProperties props, StaticContent content, ProductRepository products,
                             ReviewRepository reviews, SettingsService settings, CollectionRepository collections,
                             HomeContentService home) {
        this.props = props;
        this.content = content;
        this.products = products;
        this.reviews = reviews;
        this.settings = settings;
        this.collections = collections;
        this.home = home;
    }

    private List<CollectionDto> liveCollections() {
        return collections.findAllByActiveTrueOrderBySortOrderAsc().stream().map(CollectionDto::of).toList();
    }

    /** Everything the storefront needs to boot, in one request. Promo codes are deliberately not included. */
    @GetMapping("/store")
    public Map<String, Object> store() {
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

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("site", site);
        out.put("config", config);
        out.put("payments", Map.of("enabled", props.razorpay().enabled(), "testMode", props.razorpay().testMode()));
        out.put("home", home.get());
        out.put("collections", liveCollections());
        out.put("lines", content.lines);
        out.put("faq", content.faq);
        out.put("faqCats", content.faqCats);
        out.put("sizeChart", content.sizeChart);
        out.put("sizes", content.sizes);
        out.put("colours", content.colours);
        List<ProductDto> live = products.findAllByActiveTrueOrderByRankAsc().stream().map(ProductDto::of).toList();
        var liveIds = live.stream().map(ProductDto::id).collect(java.util.stream.Collectors.toSet());
        out.put("products", live);
        out.put("reviews", reviews.findAllByOrderByIdAsc().stream().filter(r -> liveIds.contains(r.product)).map(ReviewDto::of).toList());
        return out;
    }

    @GetMapping("/products")
    public List<ProductDto> products() {
        return products.findAllByActiveTrueOrderByRankAsc().stream().map(ProductDto::of).toList();
    }

    @GetMapping("/products/{id}")
    public ProductDto product(@PathVariable String id) {
        return products.findByIdAndActiveTrue(id).map(ProductDto::of)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Product not found."));
    }

    @GetMapping("/reviews")
    public List<ReviewDto> reviews(@RequestParam(required = false) String product) {
        var list = product == null ? reviews.findAllByOrderByIdAsc() : reviews.findByProductOrderByIdAsc(product);
        return list.stream().map(ReviewDto::of).toList();
    }

    @GetMapping("/collections")
    public List<CollectionDto> collections() {
        return liveCollections();
    }
}
