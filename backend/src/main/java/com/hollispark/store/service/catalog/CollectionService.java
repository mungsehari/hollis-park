package com.hollispark.store.service.catalog;

import com.hollispark.store.dto.catalog.CollectionDto;
import com.hollispark.store.entity.catalog.CollectionEntity;
import com.hollispark.store.exception.BadRequestException;
import com.hollispark.store.exception.ConflictException;
import com.hollispark.store.exception.ResourceNotFoundException;
import com.hollispark.store.repository.catalog.CollectionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Collections (Running, Basketball, ...) as edited in the admin panel. */
@Service
@RequiredArgsConstructor
public class CollectionService {
    private final CollectionRepository collections;
    private final StorefrontService storefront;

    @Transactional(readOnly = true)
    public List<CollectionDto> list() {
        return collections.findAllByOrderBySortOrderAsc().stream().map(CollectionDto::of).toList();
    }

    @Transactional(readOnly = true)
    public CollectionDto get(String slug) {
        return CollectionDto.of(find(slug));
    }

    @Transactional
    public CollectionDto create(CollectionDto in) {
        if (in.id() == null || in.id().isBlank()) throw new BadRequestException("Add a web address like 'hiking'.");
        if (collections.existsBySlug(in.id())) throw new ConflictException("A collection with this web address already exists.");
        CollectionEntity c = CollectionEntity.builder().slug(in.id()).build();
        in.applyTo(c);
        CollectionEntity saved = collections.saveAndFlush(c);
        storefront.changed();
        return CollectionDto.of(saved);
    }

    @Transactional
    public CollectionDto update(String slug, CollectionDto in) {
        CollectionEntity c = find(slug);
        in.applyTo(c);
        collections.flush();
        storefront.changed();
        return CollectionDto.of(c);
    }

    @Transactional
    public CollectionDto setActive(String slug, boolean active) {
        CollectionEntity c = find(slug);
        c.setActive(active);
        collections.flush();
        storefront.changed();
        return CollectionDto.of(c);
    }

    private CollectionEntity find(String slug) {
        return collections.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Collection not found."));
    }
}
