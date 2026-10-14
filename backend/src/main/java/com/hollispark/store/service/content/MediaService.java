package com.hollispark.store.service.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hollispark.store.config.StoreProperties;
import com.hollispark.store.entity.content.Media;
import com.hollispark.store.exception.ApiException;
import com.hollispark.store.repository.catalog.CollectionRepository;
import com.hollispark.store.repository.catalog.ProductRepository;
import com.hollispark.store.repository.content.CarePickRepository;
import com.hollispark.store.repository.content.MediaRepository;
import com.hollispark.store.repository.content.SiteContentRepository;
import com.hollispark.store.storage.ImageTypes;
import com.hollispark.store.storage.MediaStorage;
import java.io.IOException;
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
    private final CarePickRepository picks;
    private final ObjectMapper mapper;
    private final String baseUrl;

    public MediaService(MediaStorage storage, MediaRepository media, ProductRepository products,
                        CollectionRepository collections, SiteContentRepository content, CarePickRepository picks,
                        ObjectMapper mapper, StoreProperties props) {
        this.picks = picks;
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
        return media.save(Media.builder()
                .storageKey(key)
                .url(urlFor(key))
                .originalName(clean(file.getOriginalFilename()))
                .contentType(type.contentType())
                .sizeBytes(bytes.length)
                .alt(alt == null || alt.isBlank() ? null : alt.trim())
                .createdBy(who)
                .build());
    }

    /** Stores a bundled placeholder image once and returns its URL. */
    @Transactional
    public String seed(String fileName, byte[] bytes) {
        String key = "seed/" + fileName;
        return media.findByStorageKey(key).map(m -> {
            if (!storage.exists(key)) storage.put(key, bytes, m.getContentType());
            return m.getUrl();
        }).orElseGet(() -> {
            ImageTypes.Type type = ImageTypes.detect(bytes);
            String contentType = type == null ? "image/webp" : type.contentType();
            storage.put(key, bytes, contentType);
            return media.save(Media.builder()
                    .storageKey(key)
                    .url(urlFor(key))
                    .originalName(fileName)
                    .contentType(contentType)
                    .sizeBytes(bytes.length)
                    .createdBy("seed")
                    .build()).getUrl();
        });
    }

    @Transactional
    public void delete(UUID id) {
        Media m = media.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Image not found."));
        if (inUse(m.getUrl())) {
            throw new ApiException(HttpStatus.CONFLICT, "This image is used by a product, collection or the homepage. Replace it there first.");
        }
        storage.delete(m.getStorageKey());
        media.delete(m);
    }

    public boolean inUse(String url) {
        boolean product = products.findAll().stream().anyMatch(p -> url.equals(p.getImg()) || url.equals(p.getContextImg())
                || (p.getGallery() != null && p.getGallery().contains(url)));
        if (product) return true;
        if (collections.findAll().stream().anyMatch(c -> url.equals(c.getImage()))) return true;
        if (picks.findAll().stream().anyMatch(p -> url.equals(p.getImage()))) return true;
        return content.findAll().stream().anyMatch(c -> {
            try {
                return mapper.writeValueAsString(c.getData()).contains("\"" + url + "\"");
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
