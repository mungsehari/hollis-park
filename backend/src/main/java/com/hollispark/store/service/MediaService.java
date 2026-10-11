package com.hollispark.store.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.domain.CollectionRepository;
import com.hollispark.store.domain.Media;
import com.hollispark.store.domain.MediaRepository;
import com.hollispark.store.domain.ProductRepository;
import com.hollispark.store.domain.SiteContentRepository;
import com.hollispark.store.media.ImageTypes;
import com.hollispark.store.media.MediaStorage;
import java.io.IOException;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
    public static final long MAX_BYTES = 8L * 1024 * 1024;

    private final MediaStorage storage;
    private final MediaRepository media;
    private final ProductRepository products;
    private final CollectionRepository collections;
    private final SiteContentRepository content;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public MediaService(MediaStorage storage, MediaRepository media, ProductRepository products,
                        CollectionRepository collections, SiteContentRepository content, ObjectMapper mapper,
                        StoreProperties props) {
        this.storage = storage;
        this.media = media;
        this.products = products;
        this.collections = collections;
        this.content = content;
        this.mapper = mapper;
        String base = props.media().publicBaseUrl();
        this.baseUrl = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public String urlFor(String key) {
        return baseUrl + "/" + key;
    }

    @Transactional
    public Media upload(MultipartFile file, String alt, String who) {
        if (file == null || file.isEmpty()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Choose an image to upload.");
        if (file.getSize() > MAX_BYTES) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Images can be up to 8 MB.");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "The upload could not be read.");
        }
        ImageTypes.Type type = ImageTypes.detect(bytes);
        if (type == null) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Use a JPG, PNG, WebP or AVIF image.");
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        String key = String.format("uploads/%d/%02d/%s.%s", ym.getYear(), ym.getMonthValue(), UUID.randomUUID(), type.extension());
        storage.put(key, bytes, type.contentType());
        Media m = new Media();
        m.storageKey = key;
        m.url = urlFor(key);
        m.originalName = clean(file.getOriginalFilename());
        m.contentType = type.contentType();
        m.sizeBytes = bytes.length;
        m.alt = alt == null || alt.isBlank() ? null : alt.trim();
        m.createdAt = Instant.now();
        m.createdBy = who;
        return media.save(m);
    }

    /** Stores a bundled placeholder image once and returns its URL. */
    @Transactional
    public String seed(String fileName, byte[] bytes) {
        String key = "seed/" + fileName;
        return media.findByStorageKey(key).map(m -> {
            if (!storage.exists(key)) storage.put(key, bytes, m.contentType);
            return m.url;
        }).orElseGet(() -> {
            ImageTypes.Type type = ImageTypes.detect(bytes);
            String contentType = type == null ? "image/webp" : type.contentType();
            storage.put(key, bytes, contentType);
            Media m = new Media();
            m.storageKey = key;
            m.url = urlFor(key);
            m.originalName = fileName;
            m.contentType = contentType;
            m.sizeBytes = bytes.length;
            m.createdAt = Instant.now();
            m.createdBy = "seed";
            return media.save(m).url;
        });
    }

    @Transactional
    public void delete(Long id) {
        Media m = media.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found."));
        if (inUse(m.url)) {
            throw new ApiException(HttpStatus.CONFLICT, "This image is used by a product, collection or the homepage. Replace it there first.");
        }
        storage.delete(m.storageKey);
        media.delete(m);
    }

    public boolean inUse(String url) {
        boolean product = products.findAll().stream().anyMatch(p -> url.equals(p.img) || url.equals(p.context)
                || (p.gallery != null && p.gallery.contains(url)));
        if (product) return true;
        if (collections.findAll().stream().anyMatch(c -> url.equals(c.image))) return true;
        return content.findAll().stream().anyMatch(c -> {
            try {
                return mapper.writeValueAsString(c.data).contains("\"" + url + "\"");
            } catch (Exception e) {
                return false;
            }
        });
    }

    private static String clean(String name) {
        if (name == null) return null;
        String n = name.replaceAll("[\\r\\n\\\\/]", "_");
        return n.length() > 200 ? n.substring(n.length() - 200) : n;
    }
}
