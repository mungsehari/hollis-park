package com.hollispark.store.web;

import com.hollispark.store.domain.CollectionEntity;
import com.hollispark.store.domain.CollectionRepository;
import com.hollispark.store.domain.MediaRepository;
import com.hollispark.store.dto.AdminDtos.ActiveUpdate;
import com.hollispark.store.dto.AdminDtos.PageDto;
import com.hollispark.store.dto.ContentDtos.CollectionDto;
import com.hollispark.store.dto.ContentDtos.HomeContent;
import com.hollispark.store.dto.ContentDtos.MediaDto;
import com.hollispark.store.service.ApiException;
import com.hollispark.store.service.StorefrontService;
import com.hollispark.store.service.HomeContentService;
import com.hollispark.store.service.MediaService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Images, collections and homepage content. Reads are open to staff; changes need ADMIN. */
@RestController
@RequestMapping("/api/admin")
public class AdminContentController {
    private final MediaService media;
    private final MediaRepository mediaRepo;
    private final CollectionRepository collections;
    private final HomeContentService home;
    private final StorefrontService storefront;

    public AdminContentController(MediaService media, MediaRepository mediaRepo, CollectionRepository collections,
                                  HomeContentService home, StorefrontService storefront) {
        this.media = media;
        this.mediaRepo = mediaRepo;
        this.collections = collections;
        this.home = home;
        this.storefront = storefront;
    }

    /* ---------- media library ---------- */

    @GetMapping("/media")
    public PageDto<MediaDto> media(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "48") int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageDto.of(mediaRepo.findAll(pageable).map(MediaDto::of));
    }

    @PostMapping(value = "/media", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public MediaDto upload(@RequestPart("file") MultipartFile file, @RequestParam(required = false) String alt, Principal who) {
        return MediaDto.of(media.upload(file, alt, who.getName()));
    }

    @DeleteMapping("/media/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(@PathVariable UUID id) {
        media.delete(id);
    }

    /* ---------- collections ---------- */

    @GetMapping("/collections")
    public List<CollectionDto> collections() {
        return collections.findAllByOrderBySortOrderAsc().stream().map(CollectionDto::of).toList();
    }

    @GetMapping("/collections/{id}")
    public CollectionDto collection(@PathVariable String id) {
        return CollectionDto.of(find(id));
    }

    @PostMapping("/collections")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public CollectionDto create(@Valid @RequestBody CollectionDto in) {
        if (in.id() == null || in.id().isBlank()) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Add a web address like 'hiking'.");
        if (collections.existsBySlug(in.id())) throw new ApiException(HttpStatus.CONFLICT, "A collection with this web address already exists.");
        CollectionEntity c = CollectionEntity.builder().slug(in.id()).build();
        in.applyTo(c);
        var saved = collections.saveAndFlush(c);
        storefront.changed();
        return CollectionDto.of(saved);
    }

    @PutMapping("/collections/{id}")
    @Transactional
    public CollectionDto update(@PathVariable String id, @Valid @RequestBody CollectionDto in) {
        CollectionEntity c = find(id);
        in.applyTo(c);
        collections.flush();
        storefront.changed();
        return CollectionDto.of(c);
    }

    @PatchMapping("/collections/{id}")
    @Transactional
    public CollectionDto setActive(@PathVariable String id, @Valid @RequestBody ActiveUpdate in) {
        CollectionEntity c = find(id);
        c.setActive(in.active());
        collections.flush();
        storefront.changed();
        return CollectionDto.of(c);
    }

    /* ---------- homepage ---------- */

    @GetMapping("/home")
    public HomeContent home() {
        return home.get();
    }

    @PutMapping("/home")
    public HomeContent saveHome(@Valid @RequestBody HomeContent in) {
        var saved = home.save(in);
        storefront.changed();
        return saved;
    }

    private CollectionEntity find(String id) {
        return collections.findBySlug(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Collection not found."));
    }
}
